package com.healify.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanResponse {
    private LocalDate weekStartDate;
    private List<DayPlan> days;
    private Boolean needRegenerate;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayPlan {
        private Integer dayOfWeek;
        private String dayName;
        private List<MealItem> meals;
        private List<ExerciseItem> exercises;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MealItem {
        private String mealType;
        private String foodName;
        private Integer portionG;
        private Integer calories;
        private Double proteinG;
        private Double carbsG;
        private Double fatG;
        private String recipe;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExerciseItem {
        private String exerciseName;
        private Integer sets;
        private Integer reps;
        private Integer durationMin;
        private String intensity;
        private String notes;
    }
}
