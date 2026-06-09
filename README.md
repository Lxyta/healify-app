# Healify - 智能健康助手

AI 驱动的健康管理 Web 应用，提供个性化饮食与运动计划生成、体重追踪与智能优化。

## 技术架构

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   React 前端     │────▶│  Spring Boot    │────▶│   PostgreSQL    │
│   (Vite + TW)   │     │  后端 (8080)     │     │   数据库         │
└─────────────────┘     └────────┬────────┘     └─────────────────┘
                                 │                        │
                                 ▼                 ┌──────┴──────┐
                          ┌─────────────┐          │   Redis     │
                          │ FastAPI AI  │          │   缓存       │
                          │ 服务 (8000) │          └─────────────┘
                          └──────┬──────┘
                                 │
                    ┌────────────┴────────────┐
                    │  Claude / DeepSeek API  │
                    └─────────────────────────┘
```

## 项目结构

```
healify-app/
├── backend/          # Spring Boot 后端
├── ai-service/       # FastAPI + LangChain AI 服务
├── frontend/         # React + Tailwind 前端
├── docker-compose.yml
└── README.md
```

## 快速启动

### 1. 启动基础设施

```bash
docker-compose up -d postgres redis
```

### 2. 启动 AI 服务

```bash
cd ai-service
pip install -r requirements.txt
uvicorn main:app --port 8000 --reload
```

### 3. 启动后端

```bash
cd backend
mvnw spring-boot:run
```

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

## 功能模块

| 模块 | 说明 |
|------|------|
| 用户认证 | JWT 注册/登录 |
| 健康档案 | 身高/体重/饮食偏好/运动目标 |
| AI 计划生成 | 一周饮食+运动计划 |
| 体重追踪 | 每周记录体重，AI 分析趋势并优化计划 |
| 历史记录 | 查看历史计划和体重变化曲线 |
