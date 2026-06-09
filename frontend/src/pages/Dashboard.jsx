import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { profileAPI, weightAPI, planAPI } from '../api/client';
import { useToast } from '../components/Toast';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

const TREND_LABELS = {
  LOSING: { text: '📉 体重下降中', color: 'text-green-600', bg: 'bg-green-50' },
  GAINING: { text: '📈 体重上升中', color: 'text-red-600', bg: 'bg-red-50' },
  STABLE: { text: '📊 体重稳定', color: 'text-blue-600', bg: 'bg-blue-50' },
};

export default function Dashboard() {
  const navigate = useNavigate();
  const { toast, showToast } = useToast();
  const [profile, setProfile] = useState(null);
  const [weights, setWeights] = useState([]);
  const [recordWeight, setRecordWeight] = useState('');
  const [loading, setLoading] = useState(false);
  const [analysis, setAnalysis] = useState(null);        // AI 分析结果
  const [weekRecorded, setWeekRecorded] = useState(false); // 本周是否已记录
  const [showOptimizedPlan, setShowOptimizedPlan] = useState(false);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      const [p, w, check] = await Promise.all([
        profileAPI.get().catch(() => null),
        weightAPI.getHistory().catch(() => []),
        weightAPI.checkThisWeek().catch(() => ({ recorded: false })),
      ]);
      setProfile(p);
      setWeights(Array.isArray(w) ? w : []);
      setWeekRecorded(check.recorded);
    } catch {
      // 静默处理
    }
  };

  /** 记录体重 + AI 分析 + 优化计划（一站式） */
  const handleRecordWeight = async () => {
    const kg = parseFloat(recordWeight);
    if (!kg || kg < 20 || kg > 300) {
      showToast('请输入有效的体重（20-300 kg）', 'error');
      return;
    }
    setLoading(true);
    setAnalysis(null);
    try {
      // 调用组合接口：记录体重 + AI 分析趋势 + 优化计划
      const result = await weightAPI.recordAndOptimize({ weightKg: kg });
      setRecordWeight('');
      setWeekRecorded(true);
      setAnalysis(result);
      await loadData();
      showToast('体重已记录，AI 已分析趋势并优化计划', 'success');
    } catch (err) {
      // 如果 AI 服务不可用，至少记录体重
      try {
        await weightAPI.record({ weightKg: kg });
        setRecordWeight('');
        setWeekRecorded(true);
        await loadData();
        showToast('体重已记录（AI 服务暂不可用，请稍后手动优化计划）', 'info');
      } catch (fallbackErr) {
        showToast(err.message, 'error');
      }
    } finally {
      setLoading(false);
    }
  };

  const chartData = [...weights]
    .reverse()
    .slice(-12)
    .map((w) => ({
      date: w.recordedAt,
      weight: parseFloat(w.weightKg),
      bmi: w.bmi ? parseFloat(w.bmi) : null,
    }));

  const latestWeight = weights.length > 0 ? parseFloat(weights[0].weightKg) : null;
  const prevWeight = weights.length > 1 ? parseFloat(weights[1].weightKg) : null;
  const weightChange = latestWeight && prevWeight ? (latestWeight - prevWeight).toFixed(1) : null;

  return (
    <div>
      <h1 className="page-title">👋 欢迎回来</h1>
      {toast}

      {/* 快速状态卡 */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <div className="card text-center">
          <div className="text-3xl mb-1">⚖️</div>
          <div className="text-2xl font-bold text-healify-600">{latestWeight ?? '--'}</div>
          <div className="text-xs text-gray-500">当前体重 (kg)</div>
          {weightChange && (
            <div className={`text-xs mt-1 ${parseFloat(weightChange) < 0 ? 'text-green-500' : 'text-red-500'}`}>
              {parseFloat(weightChange) > 0 ? '+' : ''}{weightChange} kg
            </div>
          )}
        </div>
        <div className="card text-center">
          <div className="text-3xl mb-1">🎯</div>
          <div className="text-2xl font-bold text-blue-500">{profile?.targetWeightKg ?? '--'}</div>
          <div className="text-xs text-gray-500">目标体重 (kg)</div>
        </div>
        <div className="card text-center">
          <div className="text-3xl mb-1">🔥</div>
          <div className="text-2xl font-bold text-orange-500">{profile?.dailyCalorieGoal ?? '--'}</div>
          <div className="text-xs text-gray-500">每日热量 (kcal)</div>
        </div>
        <div className="card text-center">
          <div className="text-3xl mb-1">📅</div>
          <div className="text-2xl font-bold text-purple-500">{weights.length}</div>
          <div className="text-xs text-gray-500">体重记录数</div>
        </div>
      </div>

      {/* 体重记录输入卡 */}
      <div className="card mb-6">
        <h3 className="font-semibold text-gray-700 mb-3">
          {weekRecorded ? '✅ 本周体重已记录' : '📝 记录本周体重'}
        </h3>
        {weekRecorded ? (
          <p className="text-sm text-gray-500">
            本周已记录体重 {latestWeight} kg。
            下周再来记录，AI 会根据体重变化趋势自动优化你的计划。
          </p>
        ) : (
          <>
            <p className="text-sm text-gray-400 mb-3">
              每周记录一次体重，AI 会根据变化趋势自动优化你的饮食和运动计划。
            </p>
            <div className="flex gap-3">
              <input
                type="number"
                step="0.1"
                className="input-field flex-1"
                placeholder="输入体重 (kg)"
                value={recordWeight}
                onChange={(e) => setRecordWeight(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleRecordWeight()}
              />
              <button onClick={handleRecordWeight} disabled={loading} className="btn-primary">
                {loading ? '分析中...' : '记录并分析'}
              </button>
            </div>
          </>
        )}
      </div>

      {/* AI 分析结果卡 */}
      {analysis && (
        <div className="card mb-6 border-l-4 border-healify-500">
          <div className="flex items-center justify-between mb-3">
            <h3 className="font-semibold text-gray-700 flex items-center gap-2">
              🤖 AI 分析报告
            </h3>
            <span className={`text-xs px-2 py-1 rounded-full font-medium ${TREND_LABELS[analysis.trend]?.bg} ${TREND_LABELS[analysis.trend]?.color}`}>
              {TREND_LABELS[analysis.trend]?.text || analysis.trend}
            </span>
          </div>

          {/* 关键指标 */}
          <div className="grid grid-cols-3 gap-3 mb-4">
            <div className="bg-gray-50 rounded-lg p-3 text-center">
              <div className="text-xs text-gray-500 mb-1">总变化</div>
              <div className={`text-lg font-bold ${analysis.changeKg < 0 ? 'text-green-600' : analysis.changeKg > 0 ? 'text-red-600' : 'text-gray-600'}`}>
                {analysis.changeKg > 0 ? '+' : ''}{analysis.changeKg?.toFixed?.(1) ?? analysis.changeKg} kg
              </div>
            </div>
            <div className="bg-gray-50 rounded-lg p-3 text-center">
              <div className="text-xs text-gray-500 mb-1">周均速率</div>
              <div className="text-lg font-bold text-gray-700">
                {analysis.ratePerWeek != null
                  ? `${analysis.ratePerWeek > 0 ? '+' : ''}${Number(analysis.ratePerWeek).toFixed(2)} kg/周`
                  : '--'}
              </div>
            </div>
            <div className="bg-gray-50 rounded-lg p-3 text-center">
              <div className="text-xs text-gray-500 mb-1">计划调整</div>
              <div className={`text-lg font-bold ${analysis.planChanged ? 'text-healify-600' : 'text-gray-400'}`}>
                {analysis.planChanged ? '已优化' : '未变化'}
              </div>
            </div>
          </div>

          {/* AI 评估文字 */}
          {analysis.assessment && (
            <div className="bg-blue-50 border border-blue-100 rounded-lg p-3 mb-3">
              <div className="text-xs font-medium text-blue-600 mb-1">📋 趋势评估</div>
              <p className="text-sm text-gray-700 leading-relaxed">{analysis.assessment}</p>
            </div>
          )}

          {/* AI 建议文字 */}
          {analysis.suggestion && (
            <div className="bg-healify-50 border border-healify-100 rounded-lg p-3 mb-3">
              <div className="text-xs font-medium text-healify-600 mb-1">💡 优化建议</div>
              <p className="text-sm text-gray-700 leading-relaxed">{analysis.suggestion}</p>
            </div>
          )}

          {/* 计划变化摘要 */}
          {analysis.planChangeSummary && (
            <div className="bg-yellow-50 border border-yellow-100 rounded-lg p-3 mb-3">
              <div className="text-xs font-medium text-yellow-700 mb-1">🔄 计划调整</div>
              <p className="text-sm text-gray-700 leading-relaxed">{analysis.planChangeSummary}</p>
            </div>
          )}

          {/* 查看优化后计划按钮 */}
          {analysis.optimizedPlan && (
            <button
              onClick={() => {
                setShowOptimizedPlan(!showOptimizedPlan);
                if (!showOptimizedPlan) navigate('/plan');
              }}
              className="btn-primary w-full text-sm"
            >
              📅 查看优化后的周计划 →
            </button>
          )}
        </div>
      )}

      {/* 体重趋势图 */}
      <div className="card mb-6">
        <h3 className="font-semibold text-gray-700 mb-4">📈 体重变化趋势</h3>
        {chartData.length > 1 ? (
          <ResponsiveContainer width="100%" height={250}>
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
                dot={{ r: 4, fill: '#22c55e' }}
                name="体重 (kg)"
              />
            </LineChart>
          </ResponsiveContainer>
        ) : (
          <p className="text-gray-400 text-center py-8">
            记录两次以上体重后，这里将显示趋势图
          </p>
        )}
      </div>

      {/* 快捷操作 */}
      <div className="grid grid-cols-2 gap-4">
        <button onClick={() => navigate('/profile')} className="card hover:shadow-md transition-shadow text-left">
          <div className="text-2xl mb-2">📋</div>
          <div className="font-semibold text-gray-700">
            {profile ? '编辑健康档案' : '完善健康档案'}
          </div>
          <div className="text-sm text-gray-500 mt-1">
            {profile ? '更新你的身体数据和目标' : '先设置档案才能生成计划'}
          </div>
        </button>
        <button onClick={() => navigate('/plan')} className="card hover:shadow-md transition-shadow text-left">
          <div className="text-2xl mb-2">🤖</div>
          <div className="font-semibold text-gray-700">查看我的计划</div>
          <div className="text-sm text-gray-500 mt-1">
            AI 生成的饮食和运动计划
          </div>
        </button>
      </div>
    </div>
  );
}
