"""
Healify AI Service — FastAPI 入口
"""
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from routers.ai_router import router as ai_router

app = FastAPI(
    title="Healify AI Service",
    description="智能健康助手 AI 服务 — 计划生成、优化、体重分析",
    version="0.1.0",
)

# CORS — 允许 Spring Boot 后端调用
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:8080", "http://localhost:5173"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(ai_router)


@app.get("/")
async def root():
    return {"service": "Healify AI", "version": "0.1.0"}
