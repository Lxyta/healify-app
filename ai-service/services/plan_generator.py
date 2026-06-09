"""
计划生成器 — 使用 LLM 生成饮食运动计划、优化计划、分析体重趋势。
"""
import json
from typing import Optional
from models.schemas import (
    UserProfile, WeightHistory, WeightTrend,
    WeeklyPlanResponse, WeightAnalysis,
)
from services.llm_service import generate_structured_json


# ===== System Prompts =====

PLAN_SYSTEM_PROMPT = """你是一位专业的营养师和健身教练。你需要根据用户的健康档案，为其生成一周的个性化饮食和运动计划。

## 输出格式要求
你必须返回严格的 JSON 格式，结构如下：
```json
{
  "week_start_date": "YYYY-MM-DD",
  "days": [
    {
      "day_of_week": 1,
      "day_name": "周一",
      "meals": [
        {
          "meal_type": "BREAKFAST",
          "food_name": "食物名称",
          "portion_g": 200,
          "calories": 350,
          "protein_g": 20.5,
          "carbs_g": 45.0,
          "fat_g": 10.0,
          "recipe": "简单做法描述"
        }
      ],
      "exercises": [
        {
          "exercise_name": "运动名称",
          "sets": 3,
          "reps": 12,
          "duration_min": 30,
          "intensity": "MEDIUM",
          "notes": "注意事项"
        }
      ]
    }
  ]
}
```

## 设计原则
1. **饮食**：每天 4 餐（早餐、午餐、晚餐、加餐），总热量接近 daily_calorie_goal
2. **营养配比**：蛋白质优先达到 daily_protein_g，碳水 45-55%，脂肪 20-30%
3. **饮食偏好**：严格遵守 diet_preference（素食者不出现肉类）
4. **过敏原**：避开 allergies 中的食物
5. **运动**：根据 activity_level 安排，exercise_frequency 天/周，每次约 exercise_duration 分钟
6. **减重/增重**：当前体重 vs 目标体重决定热量盈余/缺口（±300-500 kcal/天）
7. 使用中文回复，菜名接地气"""


OPTIMIZE_SYSTEM_PROMPT = """你是一位专业的营养师和健身教练。用户已经执行了一周的计划，并记录了最新体重。
请根据体重变化趋势优化下周的计划。

## 输出格式
与生成计划相同，返回完整的 JSON 周计划。

## 优化原则
1. 如果体重下降过快（>1 kg/周）：适当增加热量摄入，防止代谢下降
2. 如果体重下降合理（0.3-0.8 kg/周）：基本保持当前方案，微调
3. 如果体重不变（减重目标下）：适当减少热量 200-300 kcal，或增加有氧运动
4. 如果体重上升（减重目标下）：减少热量 300-500 kcal，增加运动强度
5. 增重目标则反向调整
6. 保持饮食多样性，不要和上周完全相同"""


ANALYZE_SYSTEM_PROMPT = """你是一位专业的健康数据分析师。请分析用户的体重变化趋势并给出文字评估和建议。
返回 JSON：
```json
{
  "trend": "LOSING",
  "change_kg": -0.8,
  "rate_per_week": -0.4,
  "assessment": "过去两周体重以每周0.4kg的速度稳定下降，符合健康减重速度。",
  "suggestion": "继续保持当前饮食和运动方案，下周可适当增加蛋白质摄入以维持肌肉量。"
}
```"""


ANALYZE_AND_OPTIMIZE_PROMPT = """你是一位专业的营养师、健身教练和健康数据分析师。

你需要同时完成两项任务：
1. 分析用户的体重变化趋势
2. 根据分析结果优化下周计划

## 输出格式
返回严格 JSON：
```json
{
  "trend": "LOSING",
  "change_kg": -0.8,
  "rate_per_week": -0.4,
  "assessment": "对体重趋势的评估（2-3句话）",
  "suggestion": "对饮食和运动的调整建议（2-3句话）",
  "plan_changed": true,
  "plan_change_summary": "一句话概括优化方向，如'减少每日热量200kcal并增加一次HIIT训练'",
  "optimized_plan": {
    "week_start_date": "YYYY-MM-DD",
    "days": [ ... 与生成计划格式完全相同的7天计划 ... ]
  }
}
```

## 优化原则（重要）
根据体重变化趋势调整计划：

### 减重目标（current_weight > target_weight）：
- 下降过快（>1 kg/周）：适当增加热量 200-300 kcal，防止代谢适应和肌肉流失
- 下降合理（0.3-0.8 kg/周）：基本保持，微调食物种类增加多样性
- 体重停滞（<0.3 kg/周或不变）：减少热量 200-300 kcal，或增加有氧/替换运动类型
- 体重反弹（上升）：减少热量 300-500 kcal，增加运动强度，重新评估碳水摄入

### 增重目标（current_weight < target_weight）：
- 上升过快（>0.5 kg/周）：适当减少热量防止脂肪过多增加
- 上升合理（0.2-0.5 kg/周）：保持方案
- 停滞：增加热量 300-500 kcal，调整蛋白质/碳水比例

### 饮食多样性：
- 每周至少更换 40% 的菜品，避免用户吃腻
- 保持与用户 diet_preference 一致
- 避开 allergies 中的食物

### 运动调整：
- 每周更换 2-3 个运动项目
- 逐步提升强度（progressive overload）"""


# ===== Public Functions =====

def generate_weekly_plan(
    user_profile: UserProfile,
    weight_history: Optional[WeightHistory] = None,
) -> WeeklyPlanResponse:
    """生成一周饮食和运动计划。"""
    user_prompt = _build_profile_prompt(user_profile, weight_history)

    result = generate_structured_json(
        system_prompt=PLAN_SYSTEM_PROMPT,
        user_prompt=user_prompt,
        temperature=0.8,
    )
    return WeeklyPlanResponse(**result)


def optimize_plan(
    user_profile: UserProfile,
    weight_trend: WeightTrend,
    current_plan: dict,
) -> WeeklyPlanResponse:
    """根据体重变化优化计划。"""
    user_prompt = f"""## 用户档案
{_profile_to_text(user_profile)}

## 体重变化趋势
- 趋势方向：{weight_trend.trend or '未知'}
- 总变化量：{weight_trend.change_kg or 0} kg
- 历史记录：{json.dumps([r.model_dump() for r in weight_trend.records], ensure_ascii=False)}

## 当前计划（待优化）
{json.dumps(current_plan, ensure_ascii=False, indent=2)}

请根据体重变化优化下周计划，输出完整的 JSON。"""

    result = generate_structured_json(
        system_prompt=OPTIMIZE_SYSTEM_PROMPT,
        user_prompt=user_prompt,
        temperature=0.7,
    )
    return WeeklyPlanResponse(**result)


def analyze_and_optimize(
    user_profile: UserProfile,
    weight_trend: WeightTrend,
    current_plan: dict,
) -> dict:
    """【组合】分析体重趋势 + 生成优化后的计划（一次 LLM 调用完成）。"""
    records_text = json.dumps([r.model_dump() for r in weight_trend.records], ensure_ascii=False)

    user_prompt = f"""## 用户档案
{_profile_to_text(user_profile)}

## 体重变化趋势
- 趋势方向：{weight_trend.trend or '未知'}
- 总变化量：{weight_trend.change_kg or 0} kg
- 历史记录：{records_text}

## 当前计划
{json.dumps(current_plan, ensure_ascii=False, indent=2)}

请完成分析并生成优化后的计划。"""

    result = generate_structured_json(
        system_prompt=ANALYZE_AND_OPTIMIZE_PROMPT,
        user_prompt=user_prompt,
        temperature=0.7,
    )
    return result


def analyze_weight(
    weight_history: WeightHistory,
    user_profile: UserProfile,
) -> WeightAnalysis:
    """分析体重变化趋势。"""
    user_prompt = f"""## 用户档案
{_profile_to_text(user_profile)}

## 体重历史记录
{json.dumps([r.model_dump() for r in weight_history.records], ensure_ascii=False)}

请分析体重变化趋势并给出评估和建议。"""

    result = generate_structured_json(
        system_prompt=ANALYZE_SYSTEM_PROMPT,
        user_prompt=user_prompt,
        temperature=0.5,
    )
    return WeightAnalysis(**result)


# ===== Helpers =====

def _build_profile_prompt(profile: UserProfile, weight_history: Optional[WeightHistory]) -> str:
    prompt = f"""## 用户档案
{_profile_to_text(profile)}"""

    if weight_history and weight_history.records:
        prompt += f"\n\n## 体重历史\n{json.dumps([r.model_dump() for r in weight_history.records], ensure_ascii=False)}"
    else:
        prompt += "\n\n## 体重历史\n无历史记录，这是首次生成计划。"

    prompt += "\n\n请生成一周的饮食和运动计划，返回上述 JSON 格式。"
    return prompt


def _profile_to_text(profile: UserProfile) -> str:
    lines = []
    if profile.gender:
        lines.append(f"- 性别：{profile.gender}")
    if profile.age:
        lines.append(f"- 年龄：{profile.age}")
    if profile.height_cm:
        lines.append(f"- 身高：{profile.height_cm} cm")
    if profile.current_weight_kg:
        lines.append(f"- 当前体重：{profile.current_weight_kg} kg")
    if profile.target_weight_kg:
        lines.append(f"- 目标体重：{profile.target_weight_kg} kg")
    if profile.activity_level:
        level_map = {"SEDENTARY": "久坐", "LIGHT": "轻度活动", "MODERATE": "中等活动", "VERY_ACTIVE": "高强度"}
        lines.append(f"- 活动水平：{level_map.get(profile.activity_level, profile.activity_level)}")
    if profile.diet_preference:
        diet_map = {"OMNIVORE": "杂食", "VEGETARIAN": "素食", "VEGAN": "纯素", "KETO": "生酮"}
        lines.append(f"- 饮食偏好：{diet_map.get(profile.diet_preference, profile.diet_preference)}")
    if profile.allergies:
        lines.append(f"- 过敏原：{profile.allergies}")
    if profile.daily_calorie_goal:
        lines.append(f"- 每日热量目标：{profile.daily_calorie_goal} kcal")
    if profile.daily_protein_g:
        lines.append(f"- 每日蛋白质目标：{profile.daily_protein_g} g")
    if profile.exercise_frequency:
        lines.append(f"- 每周运动天数：{profile.exercise_frequency}")
    if profile.exercise_duration:
        lines.append(f"- 每次运动时长：{profile.exercise_duration} 分钟")

    return "\n".join(lines) if lines else "档案为空"
