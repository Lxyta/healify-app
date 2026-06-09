package com.healify.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "exercise_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExercisePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek;

    @Column(name = "exercise_name", length = 200, nullable = false)
    private String exerciseName;

    private Integer sets;

    private Integer reps;

    @Column(name = "duration_min")
    private Integer durationMin;

    @Column(length = 20)
    private String intensity;

    @Column(columnDefinition = "TEXT")
    private String notes;

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
