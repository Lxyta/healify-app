package com.healify.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

/**
 * 调用 FastAPI AI 服务的 HTTP 客户端
 */
@Slf4j
@Service
public class AIServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();
    /** 专用本地 ObjectMapper，不注入 Spring 的，避免被 Redis 序列化器的 DefaultTyping 污染 */
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${ai-service.base-url}")
    private String aiBaseUrl;

    public AIServiceClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 生成一周饮食+运动计划
     */
    public JsonNode generateWeeklyPlan(Map<String, Object> userProfile, Map<String, Object> weightHistory) {
        Map<String, Object> requestBody = Map.of(
                "user_profile", userProfile,
                "weight_history", weightHistory
        );
        return callAI("/api/ai/generate-plan", requestBody);
    }

    /**
     * 根据体重变化优化现有计划
     */
    public JsonNode optimizePlan(Map<String, Object> userProfile,
                                  Map<String, Object> weightTrend,
                                  JsonNode currentPlan) {
        Map<String, Object> requestBody = Map.of(
                "user_profile", userProfile,
                "weight_trend", weightTrend,
                "current_plan", currentPlan
        );
        return callAI("/api/ai/optimize-plan", requestBody);
    }

    /**
     * 分析体重变化趋势
     */
    public JsonNode analyzeWeightTrend(Map<String, Object> userProfile, Map<String, Object> weightHistory) {
        Map<String, Object> requestBody = Map.of(
                "user_profile", userProfile,
                "weight_history", weightHistory
        );
        return callAI("/api/ai/analyze-weight", requestBody);
    }

    /**
     * 组合调用：先分析体重趋势，再基于趋势优化计划（一次 LLM 调用完成）。
     * 返回 { trend, change_kg, rate_per_week, assessment, suggestion,
     *         plan_changed, plan_change_summary, optimized_plan: { days: [...] } }
     */
    public JsonNode analyzeAndOptimize(Map<String, Object> userProfile,
                                        Map<String, Object> weightTrend,
                                        JsonNode currentPlan) {
        Map<String, Object> requestBody = Map.of(
                "user_profile", userProfile,
                "weight_trend", weightTrend,
                "current_plan", currentPlan
        );
        return callAI("/api/ai/analyze-and-optimize", requestBody);
    }

    private JsonNode callAI(String endpoint, Map<String, Object> body) {
        // 缓存：直接存 JSON 字符串，不走 GenericJackson2JsonRedisSerializer
        String cacheKey = "ai:" + endpoint + ":" + body.hashCode();
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.info("AI 响应命中缓存: {}", cacheKey);
            try {
                return objectMapper.readTree(cached);
            } catch (Exception e) {
                log.warn("缓存 JSON 解析失败，清除缓存并重新请求: {}", e.getMessage());
                stringRedisTemplate.delete(cacheKey);
            }
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    aiBaseUrl + endpoint,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            String responseBody = response.getBody();
            JsonNode result = objectMapper.readTree(responseBody);

            // 缓存 JSON 字符串 (1 小时)
            if (result != null) {
                stringRedisTemplate.opsForValue().set(cacheKey, responseBody, Duration.ofHours(1));
            }
            return result;

        } catch (Exception e) {
            log.error("调用 AI 服务失败: {}", e.getMessage(), e);
            throw new RuntimeException("AI 服务暂时不可用，请稍后重试");
        }
    }
}
