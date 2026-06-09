package com.healify.service;

import com.healify.dto.WeightRecordDTO;
import com.healify.model.HealthProfile;
import com.healify.model.WeightRecord;
import com.healify.repository.HealthProfileRepository;
import com.healify.repository.WeightRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeightRecordService {

    private final WeightRecordRepository weightRecordRepository;
    private final HealthProfileRepository profileRepository;

    /**
     * 记录本周体重（每周限制一条，重复记录会覆盖本周已存在的记录）。
     * 同时自动同步更新 health_profile.current_weight_kg。
     */
    @Transactional
    public WeightRecord recordWeight(Long userId, WeightRecordDTO dto) {
        LocalDate recordDate = dto.getRecordedAt() != null ? dto.getRecordedAt() : LocalDate.now();
        LocalDate weekStart = recordDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);

        // 该周已有一条记录 → 更新而非新增
        List<WeightRecord> thatWeek = weightRecordRepository
                .findByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(userId, weekStart, weekEnd);
        WeightRecord record;
        boolean isUpdate = false;
        if (!thatWeek.isEmpty()) {
            record = thatWeek.get(0);
            record.setWeightKg(dto.getWeightKg());
            record.setNote(dto.getNote());
            record.setRecordedAt(recordDate);
            isUpdate = true;
            log.info("用户 {} {} 周已记录体重，覆盖更新", userId, weekStart);
        } else {
            record = WeightRecord.builder()
                    .userId(userId)
                    .weightKg(dto.getWeightKg())
                    .note(dto.getNote())
                    .recordedAt(recordDate)
                    .build();
        }

        // 计算 BMI 并同步更新档案中的当前体重
        profileRepository.findByUserId(userId).ifPresent(profile -> {
            if (profile.getHeightCm() != null) {
                BigDecimal heightM = profile.getHeightCm().divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                BigDecimal bmi = dto.getWeightKg().divide(heightM.pow(2), 1, RoundingMode.HALF_UP);
                record.setBmi(bmi);
            }
            // 同步当前体重到健康档案
            profile.setCurrentWeightKg(dto.getWeightKg());
            profileRepository.save(profile);
        });

        WeightRecord saved = weightRecordRepository.save(record);
        if (isUpdate) {
            log.info("用户 {} 本周体重已更新: {} kg, BMI: {}", userId, saved.getWeightKg(), saved.getBmi());
        }
        return saved;
    }

    public List<WeightRecord> getHistory(Long userId) {
        return weightRecordRepository.findByUserIdOrderByRecordedAtDesc(userId);
    }

    /** 获取最近 N 周的体重记录，用于 AI 趋势分析 */
    public List<WeightRecord> getRecentForAnalysis(Long userId, int weeks) {
        LocalDate since = LocalDate.now().minusWeeks(weeks);
        return weightRecordRepository.findByUserIdAndRecordedAtAfterOrderByRecordedAtAsc(userId, since);
    }

    /** 检查本周是否已记录体重 */
    public boolean hasRecordedThisWeek(Long userId) {
        LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return !weightRecordRepository
                .findByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(userId, weekStart, LocalDate.now())
                .isEmpty();
    }
}
