package com.healify.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "health_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(length = 10)
    private String gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "height_cm", precision = 5, scale = 1)
    private BigDecimal heightCm;

    @Column(name = "current_weight_kg", precision = 5, scale = 1)
    private BigDecimal currentWeightKg;

    @Column(name = "target_weight_kg", precision = 5, scale = 1)
    private BigDecimal targetWeightKg;

    @Column(name = "activity_level", length = 20)
    private String activityLevel;

    @Column(name = "diet_preference", length = 20)
    private String dietPreference;

    @Column(columnDefinition = "TEXT")
    private String allergies; // JSON array

    @Column(name = "daily_calorie_goal")
    private Integer dailyCalorieGoal;

    @Column(name = "daily_protein_g")
    private Integer dailyProteinG;

    @Column(name = "exercise_frequency")
    private Integer exerciseFrequency;

    @Column(name = "exercise_duration")
    private Integer exerciseDuration;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
