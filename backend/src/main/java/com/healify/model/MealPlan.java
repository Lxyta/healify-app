package com.healify.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "meal_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek; // 1=Mon .. 7=Sun

    @Column(name = "meal_type", length = 20, nullable = false)
    private String mealType; // BREAKFAST, LUNCH, DINNER, SNACK

    @Column(name = "food_name", length = 200, nullable = false)
    private String foodName;

    @Column(name = "portion_g")
    private Integer portionG;

    private Integer calories;

    @Column(name = "protein_g", precision = 5, scale = 1)
    private BigDecimal proteinG;

    @Column(name = "carbs_g", precision = 5, scale = 1)
    private BigDecimal carbsG;

    @Column(name = "fat_g", precision = 5, scale = 1)
    private BigDecimal fatG;

    @Column(columnDefinition = "TEXT")
    private String recipe;

    @Column(name = "generated_by_ai")
    @Builder.Default
    private Boolean generatedByAi = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
