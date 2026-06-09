package com.healify.controller;

import com.healify.dto.PlanResponse;
import com.healify.dto.WeightAnalysisResponse;
import com.healify.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    /** 生成一周计划（调用 AI） */
    @PostMapping("/generate")
    public ResponseEntity<PlanResponse> generatePlan(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(planService.generateWeeklyPlan(userId));
    }

    /** 获取当前周计划 */
    @GetMapping("/current")
    public ResponseEntity<PlanResponse> getCurrentPlan(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(planService.generateWeeklyPlan(userId));
    }

    /** 记录体重后优化计划（保留兼容） */
    @PostMapping("/optimize")
    public ResponseEntity<PlanResponse> optimizePlan(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(planService.optimizeAfterWeightRecord(userId));
    }

    /** AI 体重趋势分析（仅分析，不修改计划） */
    @GetMapping("/trend-analysis")
    public ResponseEntity<WeightAnalysisResponse> getTrendAnalysis(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(planService.getWeightTrendAnalysis(userId));
    }

    /** 历史计划 */
    @GetMapping("/history")
    public ResponseEntity<List<PlanResponse>> getHistory(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(planService.getHistory(userId));
    }
}
