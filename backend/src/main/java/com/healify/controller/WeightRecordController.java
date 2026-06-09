package com.healify.controller;

import com.healify.dto.WeightAnalysisResponse;
import com.healify.dto.WeightRecordDTO;
import com.healify.model.WeightRecord;
import com.healify.service.PlanService;
import com.healify.service.WeightRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/weight")
@RequiredArgsConstructor
public class WeightRecordController {

    private final WeightRecordService weightRecordService;
    private final PlanService planService;

    /** 记录本周体重（每周一条，重复记录覆盖更新） */
    @PostMapping
    public ResponseEntity<WeightRecord> recordWeight(Authentication auth,
                                                      @Valid @RequestBody WeightRecordDTO dto) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(weightRecordService.recordWeight(userId, dto));
    }

    /** 【核心】记录体重 + AI 分析趋势 + 优化计划（一站式） */
    @PostMapping("/record-and-optimize")
    public ResponseEntity<WeightAnalysisResponse> recordAndOptimize(Authentication auth,
                                                                     @Valid @RequestBody WeightRecordDTO dto) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(planService.recordWeightAndOptimize(userId, dto));
    }

    /** 体重历史 */
    @GetMapping("/history")
    public ResponseEntity<List<WeightRecord>> getHistory(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(weightRecordService.getHistory(userId));
    }

    /** 检查本周是否已记录 */
    @GetMapping("/check-this-week")
    public ResponseEntity<Map<String, Boolean>> checkThisWeek(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(Map.of("recorded", weightRecordService.hasRecordedThisWeek(userId)));
    }
}
