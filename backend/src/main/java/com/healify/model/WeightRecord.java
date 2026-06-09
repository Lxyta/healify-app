package com.healify.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "weight_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeightRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "weight_kg", precision = 5, scale = 1, nullable = false)
    private BigDecimal weightKg;

    @Column(precision = 4, scale = 1)
    private BigDecimal bmi;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "recorded_at", nullable = false)
    private LocalDate recordedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (recordedAt == null) {
            recordedAt = LocalDate.now();
        }
    }
}
