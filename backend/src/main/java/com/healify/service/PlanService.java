package com.healify.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.healify.dto.PlanResponse;
import com.healify.dto.PlanResponse.*;
import com.healify.dto.WeightAnalysisResponse;
import com.healify.dto.WeightRecordDTO;
import com.healify.model.*;
import com.healify.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanService {

    private final MealPlanRepository mealPlanRepository;
    private final ExercisePlanRepository exercisePlanRepository;
    private final HealthProfileRepository profileRepository;
    private final WeightRecordRepository weightRecordRepository;
    private final AIServiceClient aiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== 公开方法 ====================

    /**
     * 调用 AI 生成一周计划（若本周已有则直接返回）。
     * 首次生成时，若已有体重历史，会将趋势作为上下文传给 LLM。
     */
    @Transactional
    public PlanResponse generateWeeklyPlan(Long userId, boolean force) {
        HealthProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("请先完善健康档案"));

        LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // 若本周已有 AI 计划且未强制重建，直接返回
        List<MealPlan> existingMeals = mealPlanRepository.findByUserIdAndWeekStartDate(userId, weekStart);
        if (!force && !existingMeals.isEmpty() && existingMeals.get(0).getGeneratedByAi()) {
            return buildPlanResponse(userId, weekStart);
        }

        // 清除本周旧数据
        clearWeek(userId, weekStart);

        // 构建请求参数（体重历史作为上下文传给 LLM）
        Map<String, Object> userProfile = buildProfileMap(profile);
        List<WeightRecord> recentWeights = weightRecordRepository.findByUserIdOrderByRecordedAtDesc(userId);
        Map<String, Object> weightHistory = buildWeightHistoryMap(recentWeights);

        JsonNode aiResponse = aiClient.generateWeeklyPlan(userProfile, weightHistory);
        parseAndSavePlan(userId, weekStart, aiResponse);

        return buildPlanResponse(userId, weekStart);
    }

    /**
     * 【核心功能】记录体重 + AI 分析趋势 + 优化计划（一站式）。
     *
     * 流程：
     * 1. 获取用户档案 + 近 4 周体重记录
     * 2. 计算体重变化趋势（变化量、方向、速率）
     * 3. 构建当前计划的完整 JSON 传给 LLM
     * 4. LLM 分析趋势 + 生成优化后的新计划
     * 5. 返回分析结果 + 新计划
     */
    @Transactional
    public WeightAnalysisResponse recordWeightAndOptimize(Long userId, WeightRecordDTO weightDto) {
        HealthProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("请先完善健康档案"));

        // 1. 记录体重（WeightRecordService 会同步 profile.current_weight_kg）
        // 直接在此处理，避免循环依赖
        LocalDate recordDate = weightDto.getRecordedAt() != null ? weightDto.getRecordedAt() : LocalDate.now();
        LocalDate weekStart = recordDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);

        List<WeightRecord> existingThisWeek = weightRecordRepository
                .findByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(userId, weekStart, weekEnd);
        WeightRecord weightRecord;
        if (!existingThisWeek.isEmpty()) {
            weightRecord = existingThisWeek.get(0);
            weightRecord.setWeightKg(weightDto.getWeightKg());
            weightRecord.setNote(weightDto.getNote());
            weightRecord.setRecordedAt(recordDate);
            // 重算 BMI
            if (profile.getHeightCm() != null) {
                BigDecimal heightM = profile.getHeightCm().divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                weightRecord.setBmi(weightDto.getWeightKg().divide(heightM.pow(2), 1, RoundingMode.HALF_UP));
            }
        } else {
            weightRecord = WeightRecord.builder()
                    .userId(userId)
                    .weightKg(weightDto.getWeightKg())
                    .note(weightDto.getNote())
                    .recordedAt(recordDate)
                    .build();
            if (profile.getHeightCm() != null) {
                BigDecimal heightM = profile.getHeightCm().divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                weightRecord.setBmi(weightDto.getWeightKg().divide(heightM.pow(2), 1, RoundingMode.HALF_UP));
            }
        }
        // 同步档案体重
        profile.setCurrentWeightKg(weightDto.getWeightKg());
        profileRepository.save(profile);
        weightRecordRepository.save(weightRecord);

        // 更新 profile map 以反映最新体重
        Map<String, Object> userProfile = buildProfileMap(profile);

        // 2. 获取体重趋势（近 4 周）
        List<WeightRecord> weightHistory = weightRecordRepository.getRecentForAnalysis(userId, 4);
        Map<String, Object> weightTrend = buildWeightTrendMap(weightHistory);

        // 3. 获取当前周计划完整内容
        JsonNode currentPlanJson = buildFullCurrentPlanJson(userId, weekStart);

        // 4. 调用 AI：分析趋势 + 优化计划
        JsonNode aiResponse = aiClient.analyzeAndOptimize(userProfile, weightTrend, currentPlanJson);

        // 5. 解析分析结果
        String trend = aiResponse.has("trend") ? aiResponse.get("trend").asText() : "STABLE";
        BigDecimal changeKg = aiResponse.has("change_kg")
                ? new BigDecimal(aiResponse.get("change_kg").asText()) : BigDecimal.ZERO;
        BigDecimal ratePerWeek = aiResponse.has("rate_per_week")
                ? new BigDecimal(aiResponse.get("rate_per_week").asText()) : null;
        String assessment = aiResponse.has("assessment") ? aiResponse.get("assessment").asText() : "";
        String suggestion = aiResponse.has("suggestion") ? aiResponse.get("suggestion").asText() : "";
        String planChangeSummary = aiResponse.has("plan_change_summary")
                ? aiResponse.get("plan_change_summary").asText() : "";
        boolean planChanged = aiResponse.has("plan_changed") && aiResponse.get("plan_changed").asBoolean();

        // 6. 清除旧计划，保存优化后的新计划
        PlanResponse optimizedPlan;
        if (aiResponse.has("optimized_plan") && aiResponse.get("optimized_plan").has("days")) {
            clearWeek(userId, weekStart);
            parseAndSavePlan(userId, weekStart, aiResponse.get("optimized_plan"));
            optimizedPlan = buildPlanResponse(userId, weekStart);
        } else {
            // AI 认为无需优化，返回当前计划
            optimizedPlan = buildPlanResponse(userId, weekStart);
            planChanged = false;
        }

        return WeightAnalysisResponse.builder()
                .trend(trend)
                .changeKg(changeKg)
                .ratePerWeek(ratePerWeek)
                .assessment(assessment)
                .suggestion(suggestion)
                .optimizedPlan(optimizedPlan)
                .planChanged(planChanged)
                .planChangeSummary(planChangeSummary)
                .build();
    }

    /**
     * 仅获取体重趋势分析（不修改计划）。
     */
    public WeightAnalysisResponse getWeightTrendAnalysis(Long userId) {
        HealthProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("请先完善健康档案"));

        List<WeightRecord> weightHistory = weightRecordRepository.getRecentForAnalysis(userId, 4);
        Map<String, Object> weightHistoryMap = buildWeightHistoryMap(weightHistory);
        Map<String, Object> userProfile = buildProfileMap(profile);

        JsonNode aiResponse = aiClient.analyzeWeightTrend(userProfile, weightHistoryMap);

        String trend = aiResponse.has("trend") ? aiResponse.get("trend").asText() : "STABLE";
        BigDecimal changeKg = aiResponse.has("change_kg")
                ? new BigDecimal(aiResponse.get("change_kg").asText()) : BigDecimal.ZERO;
        BigDecimal ratePerWeek = aiResponse.has("rate_per_week")
                ? new BigDecimal(aiResponse.get("rate_per_week").asText()) : null;

        return WeightAnalysisResponse.builder()
                .trend(trend)
                .changeKg(changeKg)
                .ratePerWeek(ratePerWeek)
                .assessment(aiResponse.has("assessment") ? aiResponse.get("assessment").asText() : "")
                .suggestion(aiResponse.has("suggestion") ? aiResponse.get("suggestion").asText() : "")
                .optimizedPlan(null)
                .planChanged(false)
                .build();
    }

    /**
     * 记录体重后，AI 优化当前周计划（保留兼容旧的调用方式）。
     */
    @Transactional
    public PlanResponse optimizeAfterWeightRecord(Long userId) {
        HealthProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("请先完善健康档案"));

        LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        List<WeightRecord> weightHistory = weightRecordRepository.getRecentForAnalysis(userId, 4);
        Map<String, Object> weightTrend = buildWeightTrendMap(weightHistory);
        Map<String, Object> userProfile = buildProfileMap(profile);

        JsonNode currentPlanJson = buildFullCurrentPlanJson(userId, weekStart);

        JsonNode optimized = aiClient.optimizePlan(userProfile, weightTrend, currentPlanJson);

        clearWeek(userId, weekStart);
        parseAndSavePlan(userId, weekStart, optimized);

        return buildPlanResponse(userId, weekStart);
    }

    /** 历史计划 */
    public List<PlanResponse> getHistory(Long userId) {
        List<MealPlan> allMeals = mealPlanRepository.findByUserId(userId);
        Map<LocalDate, List<MealPlan>> grouped = allMeals.stream()
                .collect(Collectors.groupingBy(MealPlan::getWeekStartDate));

        return grouped.entrySet().stream()
                .sorted(Map.Entry.<LocalDate, List<MealPlan>>comparingByKey().reversed())
                .limit(12)
                .map(e -> buildPlanResponse(userId, e.getKey()))
                .collect(Collectors.toList());
    }

    // ==================== 私有方法 ====================

    private void clearWeek(Long userId, LocalDate weekStart) {
        mealPlanRepository.deleteByUserIdAndWeekStartDate(userId, weekStart);
        exercisePlanRepository.deleteByUserIdAndWeekStartDate(userId, weekStart);
    }

    private PlanResponse buildPlanResponse(Long userId, LocalDate weekStart) {
        List<MealPlan> meals = mealPlanRepository.findByUserIdAndWeekStartDate(userId, weekStart);
        List<ExercisePlan> exercises = exercisePlanRepository.findByUserIdAndWeekStartDate(userId, weekStart);

        Map<Integer, List<MealPlan>> mealsByDay = meals.stream()
                .collect(Collectors.groupingBy(MealPlan::getDayOfWeek));
        Map<Integer, List<ExercisePlan>> exercisesByDay = exercises.stream()
                .collect(Collectors.groupingBy(ExercisePlan::getDayOfWeek));

        List<DayPlan> days = new ArrayList<>();
        for (int d = 1; d <= 7; d++) {
            String dayName = DayOfWeek.of(d).getDisplayName(TextStyle.FULL, Locale.CHINESE);
            List<MealItem> mealItems = mealsByDay.getOrDefault(d, List.of()).stream()
                    .map(m -> MealItem.builder()
                            .mealType(m.getMealType())
                            .foodName(m.getFoodName())
                            .portionG(m.getPortionG())
                            .calories(m.getCalories())
                            .proteinG(m.getProteinG() != null ? m.getProteinG().doubleValue() : null)
                            .carbsG(m.getCarbsG() != null ? m.getCarbsG().doubleValue() : null)
                            .fatG(m.getFatG() != null ? m.getFatG().doubleValue() : null)
                            .recipe(m.getRecipe())
                            .build())
                    .collect(Collectors.toList());

            List<ExerciseItem> exerciseItems = exercisesByDay.getOrDefault(d, List.of()).stream()
                    .map(e -> ExerciseItem.builder()
                            .exerciseName(e.getExerciseName())
                            .sets(e.getSets())
                            .reps(e.getReps())
                            .durationMin(e.getDurationMin())
                            .intensity(e.getIntensity())
                            .notes(e.getNotes())
                            .build())
                    .collect(Collectors.toList());

            days.add(DayPlan.builder()
                    .dayOfWeek(d).dayName(dayName)
                    .meals(mealItems).exercises(exerciseItems)
                    .build());
        }

        // 仅当前周判断是否需要重新生成：档案有变化且计划是基于旧档案生成的
        boolean isCurrentWeek = weekStart.equals(LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
        Boolean needRegenerate = null;
        if (isCurrentWeek) {
            if (meals.isEmpty()) {
                needRegenerate = true;
            } else {
                HealthProfile profile = profileRepository.findByUserId(userId).orElse(null);
                LocalDateTime latestPlanTime = meals.stream()
                        .map(MealPlan::getCreatedAt)
                        .filter(Objects::nonNull)
                        .max(LocalDateTime::compareTo)
                        .orElse(null);
                needRegenerate = profile != null
                        && profile.getUpdatedAt() != null
                        && latestPlanTime != null
                        && profile.getUpdatedAt().isAfter(latestPlanTime);
            }
        }

        return PlanResponse.builder()
                .weekStartDate(weekStart).days(days)
                .needRegenerate(needRegenerate)
                .build();
    }

    private void parseAndSavePlan(Long userId, LocalDate weekStart, JsonNode aiResponse) {
        JsonNode days = aiResponse.get("days");
        if (days == null || !days.isArray()) return;

        for (JsonNode day : days) {
            int dayOfWeek = day.get("day_of_week").asInt();

            JsonNode meals = day.get("meals");
            if (meals != null && meals.isArray()) {
                for (JsonNode meal : meals) {
                    MealPlan mp = MealPlan.builder()
                            .userId(userId).weekStartDate(weekStart).dayOfWeek(dayOfWeek)
                            .mealType(meal.get("meal_type").asText())
                            .foodName(meal.get("food_name").asText())
                            .portionG(meal.has("portion_g") ? meal.get("portion_g").asInt() : null)
                            .calories(meal.has("calories") ? meal.get("calories").asInt() : null)
                            .proteinG(meal.has("protein_g") ? new BigDecimal(meal.get("protein_g").asText()) : null)
                            .carbsG(meal.has("carbs_g") ? new BigDecimal(meal.get("carbs_g").asText()) : null)
                            .fatG(meal.has("fat_g") ? new BigDecimal(meal.get("fat_g").asText()) : null)
                            .recipe(meal.has("recipe") ? meal.get("recipe").asText() : null)
                            .generatedByAi(true)
                            .build();
                    mealPlanRepository.save(mp);
                }
            }

            JsonNode exercises = day.get("exercises");
            if (exercises != null && exercises.isArray()) {
                for (JsonNode ex : exercises) {
                    ExercisePlan ep = ExercisePlan.builder()
                            .userId(userId).weekStartDate(weekStart).dayOfWeek(dayOfWeek)
                            .exerciseName(ex.get("exercise_name").asText())
                            .sets(ex.has("sets") ? ex.get("sets").asInt() : null)
                            .reps(ex.has("reps") ? ex.get("reps").asInt() : null)
                            .durationMin(ex.has("duration_min") ? ex.get("duration_min").asInt() : null)
                            .intensity(ex.has("intensity") ? ex.get("intensity").asText() : null)
                            .notes(ex.has("notes") ? ex.get("notes").asText() : null)
                            .generatedByAi(true)
                            .build();
                    exercisePlanRepository.save(ep);
                }
            }
        }
    }

    // ==================== 数据构建方法 ====================

    /**
     * 构建当前计划的完整 JSON（替代旧的只含 counts 的版本）。
     * LLM 可以看到每个餐食和运动项目，从而做出有意义的优化。
     */
    private JsonNode buildFullCurrentPlanJson(Long userId, LocalDate weekStart) {
        List<MealPlan> meals = mealPlanRepository.findByUserIdAndWeekStartDate(userId, weekStart);
        List<ExercisePlan> exercises = exercisePlanRepository.findByUserIdAndWeekStartDate(userId, weekStart);

        ObjectNode plan = objectMapper.createObjectNode();
        plan.put("week_start_date", weekStart.toString());
        plan.put("total_meals", meals.size());
        plan.put("total_exercises", exercises.size());

        // 每日热量汇总
        int totalCalories = meals.stream().filter(m -> m.getCalories() != null).mapToInt(MealPlan::getCalories).sum();
        plan.put("weekly_total_calories", totalCalories);

        // 完整饮食计划
        ArrayNode mealsArr = objectMapper.createArrayNode();
        for (MealPlan m : meals) {
            ObjectNode mn = objectMapper.createObjectNode();
            mn.put("day_of_week", m.getDayOfWeek());
            mn.put("meal_type", m.getMealType());
            mn.put("food_name", m.getFoodName());
            if (m.getPortionG() != null) mn.put("portion_g", m.getPortionG());
            if (m.getCalories() != null) mn.put("calories", m.getCalories());
            if (m.getProteinG() != null) mn.put("protein_g", m.getProteinG());
            if (m.getCarbsG() != null) mn.put("carbs_g", m.getCarbsG());
            if (m.getFatG() != null) mn.put("fat_g", m.getFatG());
            mealsArr.add(mn);
        }
        plan.set("meals", mealsArr);

        // 完整运动计划
        ArrayNode exArr = objectMapper.createArrayNode();
        for (ExercisePlan e : exercises) {
            ObjectNode en = objectMapper.createObjectNode();
            en.put("day_of_week", e.getDayOfWeek());
            en.put("exercise_name", e.getExerciseName());
            if (e.getSets() != null) en.put("sets", e.getSets());
            if (e.getReps() != null) en.put("reps", e.getReps());
            if (e.getDurationMin() != null) en.put("duration_min", e.getDurationMin());
            if (e.getIntensity() != null) en.put("intensity", e.getIntensity());
            exArr.add(en);
        }
        plan.set("exercises", exArr);

        return plan;
    }

    private Map<String, Object> buildProfileMap(HealthProfile profile) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("gender", profile.getGender());
        map.put("age", profile.getBirthDate() != null ?
                java.time.Period.between(profile.getBirthDate(), LocalDate.now()).getYears() : null);
        map.put("height_cm", profile.getHeightCm());
        map.put("current_weight_kg", profile.getCurrentWeightKg());
        map.put("target_weight_kg", profile.getTargetWeightKg());
        map.put("activity_level", profile.getActivityLevel());
        map.put("diet_preference", profile.getDietPreference());
        map.put("allergies", profile.getAllergies());
        map.put("daily_calorie_goal", profile.getDailyCalorieGoal());
        map.put("daily_protein_g", profile.getDailyProteinG());
        map.put("exercise_frequency", profile.getExerciseFrequency());
        map.put("exercise_duration", profile.getExerciseDuration());
        return map;
    }

    private Map<String, Object> buildWeightHistoryMap(List<WeightRecord> records) {
        List<Map<String, Object>> list = records.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", r.getRecordedAt().toString());
            m.put("weight_kg", r.getWeightKg());
            m.put("bmi", r.getBmi());
            return m;
        }).collect(Collectors.toList());
        return Map.of("records", list);
    }

    private Map<String, Object> buildWeightTrendMap(List<WeightRecord> records) {
        Map<String, Object> trend = new LinkedHashMap<>();
        trend.put("records", records.stream()
                .map(r -> Map.of("date", r.getRecordedAt().toString(), "weight_kg", r.getWeightKg()))
                .collect(Collectors.toList()));

        if (!records.isEmpty()) {
            BigDecimal first = records.get(0).getWeightKg();
            BigDecimal last = records.get(records.size() - 1).getWeightKg();
            trend.put("change_kg", last.subtract(first));
            trend.put("trend", last.compareTo(first) < 0 ? "LOSING" :
                    last.compareTo(first) > 0 ? "GAINING" : "STABLE");
            // 计算周均速率
            if (records.size() >= 2) {
                int weeks = Math.max(1, records.size() / 2);
                trend.put("rate_per_week", last.subtract(first).divide(
                        new BigDecimal(weeks), 2, RoundingMode.HALF_UP));
            }
        }
        return trend;
    }
}
