import { useState, useEffect } from 'react';
import { planAPI } from '../api/client';

const DAY_NAMES = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日'];
const MEAL_LABELS = {
  BREAKFAST: '🥐 早餐',
  LUNCH: '🍱 午餐',
  DINNER: '🍲 晚餐',
  SNACK: '🍎 加餐',
};

export default function PlanView() {
  const [plan, setPlan] = useState(null);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [error, setError] = useState('');
  const [activeDay, setActiveDay] = useState(
    new Date().getDay() === 0 ? 7 : new Date().getDay()
  ); // 默认显示今天

  useEffect(() => {
    loadPlan();
  }, []);

  const loadPlan = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await planAPI.getCurrent();
      setPlan(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleGenerate = async () => {
    setGenerating(true);
    setError('');
    try {
      const data = await planAPI.generate(true);
      setPlan(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setGenerating(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <div className="text-gray-400 text-lg animate-pulse">加载中...</div>
      </div>
    );
  }

  if (error && !plan) {
    return (
      <div className="text-center py-20">
        <div className="text-6xl mb-4">🤖</div>
        <h2 className="text-xl font-semibold text-gray-700 mb-2">还没有计划</h2>
        <p className="text-gray-500 mb-6">
          {error.includes('健康档案') ? '请先完善健康档案' : '让 AI 为你生成一周饮食和运动计划'}
        </p>
        <button onClick={handleGenerate} disabled={generating} className="btn-primary text-lg px-8 py-3">
          {generating ? '⏳ AI 生成中...' : '🚀 生成我的计划'}
        </button>
        {error && !error.includes('健康档案') && (
          <p className="text-red-500 text-sm mt-4">{error}</p>
        )}
      </div>
    );
  }

  const currentDayPlan = plan?.days?.find((d) => d.dayOfWeek === activeDay);

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="page-title mb-0">📅 我的计划</h1>
        {plan?.needRegenerate && (
          <button onClick={handleGenerate} disabled={generating} className="btn-secondary text-sm">
            {generating ? '生成中...' : '🔄 重新生成'}
          </button>
        )}
      </div>

      {plan?.weekStartDate && (
        <p className="text-sm text-gray-500 -mt-4 mb-4">
          计划周期：{plan.weekStartDate} 起
        </p>
      )}

      {/* 星期选择 */}
      <div className="flex gap-1 mb-6 overflow-x-auto pb-2">
        {DAY_NAMES.slice(1).map((name, i) => {
          const day = i + 1;
          const isToday = new Date().getDay() === (day % 7);
          const isActive = activeDay === day;
          return (
            <button
              key={day}
              onClick={() => setActiveDay(day)}
              className={`flex-shrink-0 px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                isActive
                  ? 'bg-healify-600 text-white shadow-sm'
                  : 'bg-white text-gray-600 hover:bg-healify-50 border border-gray-200'
              }`}
            >
              {name}
              {isToday && ' · 今天'}
            </button>
          );
        })}
      </div>

      {/* 当日计划 */}
      {currentDayPlan ? (
        <div className="space-y-6">
          {/* 饮食 */}
          <div className="card">
            <h3 className="font-semibold text-gray-700 mb-4 text-lg">🍽️ {DAY_NAMES[activeDay]} 饮食</h3>
            {currentDayPlan.meals.length > 0 ? (
              <div className="space-y-3">
                {currentDayPlan.meals.map((meal, idx) => (
                  <div key={idx} className="flex items-start gap-4 p-3 bg-gray-50 rounded-lg">
                    <div className="text-sm font-medium text-gray-500 w-16 flex-shrink-0 pt-0.5">
                      {MEAL_LABELS[meal.mealType] || meal.mealType}
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="font-medium text-gray-800">{meal.foodName}</div>
                      <div className="flex flex-wrap gap-x-4 gap-y-1 mt-1 text-xs text-gray-500">
                        {meal.portionG && <span>📏 {meal.portionG}g</span>}
                        {meal.calories && <span>🔥 {meal.calories} kcal</span>}
                        {meal.proteinG != null && <span>🥩 蛋白质 {meal.proteinG}g</span>}
                        {meal.carbsG != null && <span>🍚 碳水 {meal.carbsG}g</span>}
                        {meal.fatG != null && <span>🧈 脂肪 {meal.fatG}g</span>}
                      </div>
                      {meal.recipe && (
                        <details className="mt-2">
                          <summary className="text-xs text-healify-600 cursor-pointer hover:underline">
                            查看做法
                          </summary>
                          <p className="text-xs text-gray-500 mt-1 bg-white p-2 rounded border">
                            {meal.recipe}
                          </p>
                        </details>
                      )}
                    </div>
                  </div>
                ))}
                {/* 当日营养汇总 */}
                <div className="pt-3 border-t border-gray-100 flex flex-wrap gap-4 text-sm">
                  <span className="font-medium text-gray-700">
                    🔥 总计：{currentDayPlan.meals.reduce((s, m) => s + (m.calories || 0), 0)} kcal
                  </span>
                  <span className="text-gray-500">
                    🥩 {currentDayPlan.meals.reduce((s, m) => s + (m.proteinG || 0), 0).toFixed(1)}g 蛋白质
                  </span>
                </div>
              </div>
            ) : (
              <p className="text-gray-400 text-center py-4">当日暂无饮食计划</p>
            )}
          </div>

          {/* 运动 */}
          <div className="card">
            <h3 className="font-semibold text-gray-700 mb-4 text-lg">🏋️ {DAY_NAMES[activeDay]} 运动</h3>
            {currentDayPlan.exercises.length > 0 ? (
              <div className="space-y-3">
                {currentDayPlan.exercises.map((ex, idx) => (
                  <div key={idx} className="flex items-start gap-4 p-3 bg-gray-50 rounded-lg">
                    <div className="flex-1">
                      <div className="font-medium text-gray-800">{ex.exerciseName}</div>
                      <div className="flex flex-wrap gap-x-4 gap-y-1 mt-1 text-xs text-gray-500">
                        {ex.sets && ex.reps && <span>🔁 {ex.sets}组 × {ex.reps}次</span>}
                        {ex.durationMin && <span>⏱️ {ex.durationMin} 分钟</span>}
                        {ex.intensity && (
                          <span className={`px-1.5 py-0.5 rounded text-xs font-medium ${
                            ex.intensity === 'HIGH' ? 'bg-red-100 text-red-600' :
                            ex.intensity === 'MEDIUM' ? 'bg-yellow-100 text-yellow-700' :
                            'bg-green-100 text-green-600'
                          }`}>
                            {ex.intensity === 'HIGH' ? '高强度' :
                             ex.intensity === 'MEDIUM' ? '中等' : '低强度'}
                          </span>
                        )}
                      </div>
                      {ex.notes && (
                        <p className="text-xs text-gray-400 mt-1">💡 {ex.notes}</p>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-gray-400 text-center py-4">休息日 ♨️</p>
            )}
          </div>
        </div>
      ) : (
        <p className="text-gray-400 text-center py-10">选择上方日期查看计划</p>
      )}
    </div>
  );
}
