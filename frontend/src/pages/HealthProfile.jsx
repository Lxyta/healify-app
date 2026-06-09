import { useState, useEffect } from 'react';
import { profileAPI } from '../api/client';
import { useToast } from '../components/Toast';

const ACTIVITY_OPTIONS = [
  { value: 'SEDENTARY', label: '🪑 久坐 — 几乎不运动' },
  { value: 'LIGHT', label: '🚶 轻度 — 每周 1-2 次' },
  { value: 'MODERATE', label: '🏃 中等 — 每周 3-5 次' },
  { value: 'VERY_ACTIVE', label: '💪 高强度 — 每周 6-7 次' },
];

const DIET_OPTIONS = [
  { value: 'OMNIVORE', label: '🥩 杂食' },
  { value: 'VEGETARIAN', label: '🥬 素食' },
  { value: 'VEGAN', label: '🌱 纯素' },
  { value: 'KETO', label: '🥑 生酮' },
];

export default function HealthProfile() {
  const [profile, setProfile] = useState({
    gender: '',
    birthDate: '',
    heightCm: '',
    currentWeightKg: '',
    targetWeightKg: '',
    activityLevel: 'MODERATE',
    dietPreference: 'OMNIVORE',
    allergies: '',
    dailyCalorieGoal: '',
    dailyProteinG: '',
    exerciseFrequency: '3',
    exerciseDuration: '45',
  });
  const [loading, setLoading] = useState(false);
  const { toast, showToast } = useToast();

  useEffect(() => {
    profileAPI.get()
      .then((data) => {
        if (data) {
          setProfile((prev) => ({
            ...prev,
            gender: data.gender || '',
            birthDate: data.birthDate || '',
            heightCm: data.heightCm ?? '',
            currentWeightKg: data.currentWeightKg ?? '',
            targetWeightKg: data.targetWeightKg ?? '',
            activityLevel: data.activityLevel || 'MODERATE',
            dietPreference: data.dietPreference || 'OMNIVORE',
            allergies: data.allergies || '',
            dailyCalorieGoal: data.dailyCalorieGoal ?? '',
            dailyProteinG: data.dailyProteinG ?? '',
            exerciseFrequency: data.exerciseFrequency ?? '3',
            exerciseDuration: data.exerciseDuration ?? '45',
          }));
        }
      })
      .catch(() => {}); // 未创建则使用默认值
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      await profileAPI.save({
        ...profile,
        heightCm: profile.heightCm ? parseFloat(profile.heightCm) : null,
        currentWeightKg: profile.currentWeightKg ? parseFloat(profile.currentWeightKg) : null,
        targetWeightKg: profile.targetWeightKg ? parseFloat(profile.targetWeightKg) : null,
        dailyCalorieGoal: profile.dailyCalorieGoal ? parseInt(profile.dailyCalorieGoal) : null,
        dailyProteinG: profile.dailyProteinG ? parseInt(profile.dailyProteinG) : null,
        exerciseFrequency: parseInt(profile.exerciseFrequency),
        exerciseDuration: parseInt(profile.exerciseDuration),
      });
      showToast('健康档案保存成功！', 'success');
    } catch (err) {
      showToast(err.message, 'error');
    } finally {
      setLoading(false);
    }
  };

  const update = (field) => (e) => setProfile({ ...profile, [field]: e.target.value });

  return (
    <div>
      <h1 className="page-title">📋 健康档案</h1>
      <p className="text-gray-500 mb-6 text-sm">
        完善你的身体数据，AI 将据此生成个性化饮食和运动计划。
      </p>

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* 基本信息 */}
        <div className="card">
          <h3 className="font-semibold text-gray-700 mb-4">👤 基本信息</h3>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-600 mb-1">性别</label>
              <select className="input-field" value={profile.gender} onChange={update('gender')}>
                <option value="">请选择</option>
                <option value="MALE">男</option>
                <option value="FEMALE">女</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-600 mb-1">出生日期</label>
              <input type="date" className="input-field" value={profile.birthDate} onChange={update('birthDate')} />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-600 mb-1">身高 (cm)</label>
              <input type="number" step="0.1" className="input-field" placeholder="例: 170.0"
                     value={profile.heightCm} onChange={update('heightCm')} />
            </div>
          </div>
        </div>

        {/* 体重目标 */}
        <div className="card">
          <h3 className="font-semibold text-gray-700 mb-4">⚖️ 体重目标</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-600 mb-1">当前体重 (kg)</label>
              <input type="number" step="0.1" className="input-field" placeholder="例: 70.0"
                     value={profile.currentWeightKg} onChange={update('currentWeightKg')} />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-600 mb-1">目标体重 (kg)</label>
              <input type="number" step="0.1" className="input-field" placeholder="例: 65.0"
                     value={profile.targetWeightKg} onChange={update('targetWeightKg')} />
            </div>
          </div>
        </div>

        {/* 饮食偏好 */}
        <div className="card">
          <h3 className="font-semibold text-gray-700 mb-4">🍽️ 饮食偏好</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-600 mb-1">饮食类型</label>
              <select className="input-field" value={profile.dietPreference} onChange={update('dietPreference')}>
                {DIET_OPTIONS.map((o) => (
                  <option key={o.value} value={o.value}>{o.label}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-600 mb-1">过敏原</label>
              <input type="text" className="input-field" placeholder="例: 花生, 牛奶, 海鲜 (逗号分隔)"
                     value={profile.allergies} onChange={update('allergies')} />
            </div>
          </div>
        </div>

        {/* 营养目标 */}
        <div className="card">
          <h3 className="font-semibold text-gray-700 mb-4">🎯 营养目标</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-600 mb-1">
                每日热量目标 (kcal)
              </label>
              <input type="number" className="input-field" placeholder="例: 2000"
                     value={profile.dailyCalorieGoal} onChange={update('dailyCalorieGoal')} />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-600 mb-1">
                每日蛋白质 (g)
              </label>
              <input type="number" className="input-field" placeholder="例: 80"
                     value={profile.dailyProteinG} onChange={update('dailyProteinG')} />
            </div>
          </div>
        </div>

        {/* 运动偏好 */}
        <div className="card">
          <h3 className="font-semibold text-gray-700 mb-4">🏋️ 运动偏好</h3>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-600 mb-1">活动水平</label>
              <select className="input-field" value={profile.activityLevel} onChange={update('activityLevel')}>
                {ACTIVITY_OPTIONS.map((o) => (
                  <option key={o.value} value={o.value}>{o.label}</option>
                ))}
              </select>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-600 mb-1">每周运动天数</label>
                <input type="number" min="1" max="7" className="input-field"
                       value={profile.exerciseFrequency} onChange={update('exerciseFrequency')} />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-600 mb-1">每次运动时长 (分钟)</label>
                <input type="number" min="10" max="180" className="input-field"
                       value={profile.exerciseDuration} onChange={update('exerciseDuration')} />
              </div>
            </div>
          </div>
        </div>

        <button type="submit" disabled={loading} className="btn-primary w-full py-3 text-lg">
          {loading ? '保存中...' : '💾 保存档案'}
        </button>
      </form>
      {toast}
    </div>
  );
}
