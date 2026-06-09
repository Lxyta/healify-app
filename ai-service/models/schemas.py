"""
Pydantic models for request/response validation.
"""
from pydantic import BaseModel, Field
from typing import Optional, List
from decimal import Decimal


# ===== Request Schemas =====

class UserProfile(BaseModel):
    gender: Optional[str] = None
    age: Optional[int] = None
    height_cm: Optional[float] = None
    current_weight_kg: Optional[float] = None
    target_weight_kg: Optional[float] = None
    activity_level: Optional[str] = None       # SEDENTARY | LIGHT | MODERATE | VERY_ACTIVE
    diet_preference: Optional[str] = None       # OMNIVORE | VEGETARIAN | VEGAN | KETO
    allergies: Optional[str] = None             # JSON array string
    daily_calorie_goal: Optional[int] = None
    daily_protein_g: Optional[int] = None
    exercise_frequency: Optional[int] = None    # days per week
    exercise_duration: Optional[int] = None     # minutes per session


class WeightEntry(BaseModel):
    date: str
    weight_kg: float


class WeightHistory(BaseModel):
    records: List[WeightEntry] = []


class GeneratePlanRequest(BaseModel):
    user_profile: UserProfile
    weight_history: Optional[WeightHistory] = None


class WeightTrend(BaseModel):
    records: List[WeightEntry] = []
    change_kg: Optional[float] = None
    trend: Optional[str] = None  # LOSING | GAINING | STABLE


class OptimizePlanRequest(BaseModel):
    user_profile: UserProfile
    weight_trend: WeightTrend
    current_plan: dict  # 当前计划 JSON


class AnalyzeWeightRequest(BaseModel):
    weight_history: WeightHistory
    user_profile: UserProfile


# ===== Response Schemas =====

class MealItem(BaseModel):
    meal_type: str                # BREAKFAST | LUNCH | DINNER | SNACK
    food_name: str
    portion_g: Optional[int] = None
    calories: Optional[int] = None
    protein_g: Optional[float] = None
    carbs_g: Optional[float] = None
    fat_g: Optional[float] = None
    recipe: Optional[str] = None


class ExerciseItem(BaseModel):
    exercise_name: str
    sets: Optional[int] = None
    reps: Optional[int] = None
    duration_min: Optional[int] = None
    intensity: Optional[str] = None
    notes: Optional[str] = None


class DayPlan(BaseModel):
    day_of_week: int              # 1=Mon .. 7=Sun
    day_name: str
    meals: List[MealItem] = []
    exercises: List[ExerciseItem] = []


class WeeklyPlanResponse(BaseModel):
    week_start_date: str
    days: List[DayPlan] = []


class WeightAnalysis(BaseModel):
    trend: str                    # LOSING | GAINING | STABLE
    change_kg: float
    rate_per_week: Optional[float] = None
    assessment: str               # AI 评估文字
    suggestion: str               # AI 建议文字
