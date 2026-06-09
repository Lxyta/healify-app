package com.healify.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * AI 体重趋势分析 + 优化后的计划
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeightAnalysisResponse {
    private String trend;              // LOSING | GAINING | STABLE
    private BigDecimal changeKg;       // 总变化量
    private BigDecimal ratePerWeek;    // 每周变化速率
    private String assessment;         // AI 评估文字
    private String suggestion;         // AI 建议文字
    private PlanResponse optimizedPlan; // 优化后的新计划
    private boolean planChanged;       // 计划是否有实质变化
    private String planChangeSummary;  // 计划变化摘要（AI 生成的简要说明）
}
