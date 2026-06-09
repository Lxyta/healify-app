package com.healify.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class WeightRecordDTO {
    @NotNull
    @DecimalMin("20.0") @DecimalMax("300.0")
    private BigDecimal weightKg;

    private String note;

    /** 可选：指定记录日期，不传则默认当天 */
    private LocalDate recordedAt;
}
