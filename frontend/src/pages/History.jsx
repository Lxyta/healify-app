import { useState, useEffect } from 'react';
import { weightAPI, planAPI } from '../api/client';
import { useToast } from '../components/Toast';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

export default function History() {
  const [weights, setWeights] = useState([]);
  const [plans, setPlans] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('weight');
  const [trendAnalysis, setTrendAnalysis] = useState(null);
  const [analyzing, setAnalyzing] = useState(false);
  const { toast, showToast } = useToast();

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    try {
      const [w, p] = await Promise.all([
        weightAPI.getHistory().catch(() => []),
        planAPI.getHistory().catch(() => []),
      ]);
      setWeights(Array.isArray(w) ? w : []);
      setPlans(Array.isArray(p) ? p : []);
    } finally {
      setLoading(false);
    }
  };

  const chartData = [...weights]
    .reverse()
    .map((w) => ({
      date: w.recordedAt,
      weight: parseFloat(w.weightKg),
      bmi: w.bmi ? parseFloat(w.bmi) : null,
    }));

  // 计算统计
  const stats = {
    total: weights.length,
    latest: weights.length > 0 ? parseFloat(weights[0].weightKg) : null,
    min: weights.length > 0 ? Math.min(...weights.map((w) => parseFloat(w.weightKg))) : null,
    max: weights.length > 0 ? Math.max(...weights.map((w) => parseFloat(w.weightKg))) : null,
    first: weights.length > 0 ? parseFloat(weights[weights.length - 1].weightKg) : null,
  };
  const totalChange = stats.latest && stats.first ? (stats.latest - stats.first).toFixed(1) : null;

  const loadTrendAnalysis = async () => {
    setAnalyzing(true);
    try {
      const result = await weightAPI.getTrendAnalysis();
      setTrendAnalysis(result);
    } catch (err) {
      showToast(err.message, 'error');
    } finally {
      setAnalyzing(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <div className="text-gray-400 text-lg animate-pulse">加载中...</div>
      </div>
    );
  }

  return (
    <div>
      <h1 className="page-title">📊 历史记录</h1>
      {toast}

      {/* Tabs */}
      <div className="flex gap-2 mb-6">
        {[
          { key: 'weight', label: '⚖️ 体重记录' },
          { key: 'plan', label: '📅 历史计划' },
        ].map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
              activeTab === tab.key
                ? 'bg-healify-600 text-white'
                : 'bg-white text-gray-600 border border-gray-200 hover:bg-healify-50'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {activeTab === 'weight' && (
        <div className="space-y-6">
          {/* 体重统计 */}
          {weights.length > 0 && (
            <>
              <div className="card">
                <h3 className="font-semibold text-gray-700 mb-4">📈 体重趋势图</h3>
                <ResponsiveContainer width="100%" height={300}>
                  <LineChart data={chartData}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                    <XAxis dataKey="date" tick={{ fontSize: 12 }} />
                    <YAxis domain={['auto', 'auto']} tick={{ fontSize: 12 }} />
                    <Tooltip />
                    <Line
                      type="monotone"
                      dataKey="weight"
                      stroke="#22c55e"
                      strokeWidth={2}
                      dot={{ r: 5, fill: '#22c55e' }}
                      name="体重 (kg)"
                    />
                  </LineChart>
                </ResponsiveContainer>
              </div>

              <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
                <div className="card text-center py-3">
                  <div className="text-xs text-gray-500">记录次数</div>
                  <div className="text-xl font-bold text-gray-700">{stats.total}</div>
                </div>
                <div className="card text-center py-3">
                  <div className="text-xs text-gray-500">最新体重</div>
                  <div className="text-xl font-bold text-healify-600">{stats.latest} kg</div>
                </div>
                <div className="card text-center py-3">
                  <div className="text-xs text-gray-500">最低</div>
                  <div className="text-xl font-bold text-blue-500">{stats.min} kg</div>
                </div>
                <div className="card text-center py-3">
                  <div className="text-xs text-gray-500">最高</div>
                  <div className="text-xl font-bold text-red-500">{stats.max} kg</div>
                </div>
                <div className="card text-center py-3">
                  <div className="text-xs text-gray-500">总变化</div>
                  <div className={`text-xl font-bold ${parseFloat(totalChange) < 0 ? 'text-green-500' : 'text-red-500'}`}>
                    {parseFloat(totalChange) > 0 ? '+' : ''}{totalChange} kg
                  </div>
                </div>
              </div>
            </>
          )}

          {/* AI 趋势分析 */}
          <div className="card">
            <div className="flex items-center justify-between mb-3">
              <h3 className="font-semibold text-gray-700">🤖 AI 趋势分析</h3>
              <button
                onClick={loadTrendAnalysis}
                disabled={analyzing || weights.length < 2}
                className="btn-primary text-xs"
              >
                {analyzing ? '分析中...' : trendAnalysis ? '🔄 重新分析' : '开始分析'}
              </button>
            </div>
            {weights.length < 2 && (
              <p className="text-sm text-gray-400">需要至少 2 次体重记录才能进行趋势分析</p>
            )}
            {trendAnalysis && (
              <div className="space-y-3 mt-3">
                <div className="grid grid-cols-3 gap-3">
                  <div className="bg-gray-50 rounded-lg p-3 text-center">
                    <div className="text-xs text-gray-500 mb-1">趋势</div>
                    <div className={`text-lg font-bold ${
                      trendAnalysis.trend === 'LOSING' ? 'text-green-600' :
                      trendAnalysis.trend === 'GAINING' ? 'text-red-600' : 'text-blue-600'
                    }`}>
                      {trendAnalysis.trend === 'LOSING' ? '📉 下降' :
                       trendAnalysis.trend === 'GAINING' ? '📈 上升' : '📊 稳定'}
                    </div>
                  </div>
                  <div className="bg-gray-50 rounded-lg p-3 text-center">
                    <div className="text-xs text-gray-500 mb-1">总变化</div>
                    <div className="text-lg font-bold text-gray-700">
                      {trendAnalysis.changeKg > 0 ? '+' : ''}{Number(trendAnalysis.changeKg).toFixed(1)} kg
                    </div>
                  </div>
                  <div className="bg-gray-50 rounded-lg p-3 text-center">
                    <div className="text-xs text-gray-500 mb-1">周均速率</div>
                    <div className="text-lg font-bold text-gray-700">
                      {trendAnalysis.ratePerWeek != null
                        ? `${Number(trendAnalysis.ratePerWeek) > 0 ? '+' : ''}${Number(trendAnalysis.ratePerWeek).toFixed(2)}`
                        : '--'} kg/周
                    </div>
                  </div>
                </div>
                {trendAnalysis.assessment && (
                  <div className="bg-blue-50 rounded-lg p-3 text-sm text-gray-700">{trendAnalysis.assessment}</div>
                )}
                {trendAnalysis.suggestion && (
                  <div className="bg-healify-50 rounded-lg p-3 text-sm text-gray-700">{trendAnalysis.suggestion}</div>
                )}
              </div>
            )}
          </div>

          {/* 体重列表 */}
          <div className="card">
            <h3 className="font-semibold text-gray-700 mb-4">📋 体重记录列表</h3>
            {weights.length > 0 ? (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="text-left text-gray-500 border-b">
                      <th className="pb-3 font-medium">日期</th>
                      <th className="pb-3 font-medium">体重 (kg)</th>
                      <th className="pb-3 font-medium">BMI</th>
                      <th className="pb-3 font-medium">备注</th>
                    </tr>
                  </thead>
                  <tbody>
                    {weights.map((w) => (
                      <tr key={w.id} className="border-b border-gray-50 hover:bg-gray-50">
                        <td className="py-3 text-gray-700">{w.recordedAt}</td>
                        <td className="py-3 font-medium text-gray-800">{parseFloat(w.weightKg).toFixed(1)}</td>
                        <td className="py-3 text-gray-500">{w.bmi ? parseFloat(w.bmi).toFixed(1) : '-'}</td>
                        <td className="py-3 text-gray-400">{w.note || '-'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <p className="text-gray-400 text-center py-8">暂无体重记录</p>
            )}
          </div>
        </div>
      )}

      {activeTab === 'plan' && (
        <div className="space-y-4">
          {plans.length > 0 ? (
            plans.map((plan, idx) => (
              <details key={idx} className="card group">
                <summary className="cursor-pointer list-none flex items-center justify-between">
                  <span className="font-semibold text-gray-700">
                    📅 {plan.weekStartDate} 起的一周计划
                  </span>
                  <span className="text-gray-400 group-open:rotate-180 transition-transform">▼</span>
                </summary>
                <div className="mt-4 space-y-3">
                  {plan.days?.map((day) => (
                    <div key={day.dayOfWeek} className="border-t border-gray-100 pt-3">
                      <h4 className="font-medium text-gray-600 mb-2">{day.dayName}</h4>
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                        <div>
                          <p className="text-xs text-gray-400 mb-1">饮食</p>
                          {day.meals?.slice(0, 3).map((m, i) => (
                            <p key={i} className="text-sm text-gray-600">
                              {m.mealType === 'BREAKFAST' ? '早' :
                               m.mealType === 'LUNCH' ? '午' :
                               m.mealType === 'DINNER' ? '晚' : '加'}：
                              {m.foodName} ({m.calories}kcal)
                            </p>
                          ))}
                          {day.meals?.length > 3 && (
                            <p className="text-xs text-gray-400">...还有 {day.meals.length - 3} 餐</p>
                          )}
                        </div>
                        <div>
                          <p className="text-xs text-gray-400 mb-1">运动</p>
                          {day.exercises?.map((ex, i) => (
                            <p key={i} className="text-sm text-gray-600">
                              {ex.exerciseName}
                              {ex.durationMin && ` ${ex.durationMin}分钟`}
                            </p>
                          ))}
                          {day.exercises?.length === 0 && (
                            <p className="text-sm text-gray-400">休息日</p>
                          )}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </details>
            ))
          ) : (
            <div className="text-center py-16">
              <div className="text-5xl mb-4">📭</div>
              <p className="text-gray-500">暂无历史计划</p>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
