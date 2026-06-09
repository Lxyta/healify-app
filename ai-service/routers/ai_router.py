"""
AI 服务路由 — 暴露 API 给 Spring Boot 后端调用。
"""
from fastapi import APIRouter, HTTPException
from models.schemas import (
    GeneratePlanRequest,
    OptimizePlanRequest,
    AnalyzeWeightRequest,
    WeeklyPlanResponse,
    WeightAnalysis,
)
from services.plan_generator import (
    generate_weekly_plan,
    optimize_plan,
    analyze_weight,
    analyze_and_optimize,
)

router = APIRouter(prefix="/api/ai", tags=["AI"])


@router.post("/generate-plan", response_model=WeeklyPlanResponse)
async def generate_plan(request: GeneratePlanRequest):
    """生成一周饮食+运动计划。"""
    try:
        return generate_weekly_plan(
            user_profile=request.user_profile,
            weight_history=request.weight_history,
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"AI 生成失败: {str(e)}")


@router.post("/optimize-plan", response_model=WeeklyPlanResponse)
async def optimize_plan_endpoint(request: OptimizePlanRequest):
    """根据体重变化优化计划。"""
    try:
        return optimize_plan(
            user_profile=request.user_profile,
            weight_trend=request.weight_trend,
            current_plan=request.current_plan,
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"AI 优化失败: {str(e)}")


@router.post("/analyze-weight", response_model=WeightAnalysis)
async def analyze_weight_endpoint(request: AnalyzeWeightRequest):
    """分析体重变化趋势。"""
    try:
        return analyze_weight(
            weight_history=request.weight_history,
            user_profile=request.user_profile,
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"AI 分析失败: {str(e)}")


@router.post("/analyze-and-optimize")
async def analyze_and_optimize_endpoint(request: OptimizePlanRequest):
    """【组合】分析体重趋势 + 生成优化后的计划（一次 LLM 调用）。"""
    try:
        result = analyze_and_optimize(
            user_profile=request.user_profile,
            weight_trend=request.weight_trend,
            current_plan=request.current_plan,
        )
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"AI 分析与优化失败: {str(e)}")


@router.get("/health")
async def health_check():
    from config import settings
    return {"status": "ok", "provider": settings.LLM_PROVIDER}
