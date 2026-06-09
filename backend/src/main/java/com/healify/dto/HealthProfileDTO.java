package com.healify.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class HealthProfileDTO {
    private String gender;

    private LocalDate birthDate;

    @DecimalMin("50.0") @DecimalMax("250.0")
    private BigDecimal heightCm;

    @DecimalMin("20.0") @DecimalMax("300.0")
    private BigDecimal currentWeightKg;

    @DecimalMin("20.0") @DecimalMax("300.0")
    private BigDecimal targetWeightKg;

    private String activityLevel;   // SEDENTARY, LIGHT, MODERATE, VERY_ACTIVE
    private String dietPreference;  // OMNIVORE, VEGETARIAN, VEGAN, KETO
    private String allergies;       // JSON array string

    @Min(500) @Max(5000)
    private Integer dailyCalorieGoal;

    @Min(20) @Max(300)
    private Integer dailyProteinG;

    @Min(1) @Max(7)
    private Integer exerciseFrequency;

    @Min(10) @Max(180)
    private Integer exerciseDuration;
}
