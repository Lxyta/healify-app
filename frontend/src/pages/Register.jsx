import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authAPI } from '../api/client';

export default function Register() {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
    nickname: '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (form.password !== form.confirmPassword) {
      setError('两次密码输入不一致');
      return;
    }

    setLoading(true);
    try {
      const data = await authAPI.register({
        username: form.username,
        email: form.email,
        password: form.password,
        nickname: form.nickname || form.username,
      });
      localStorage.setItem('token', data.token);
      localStorage.setItem('user', JSON.stringify(data));
      navigate('/profile'); // 注册后先去完善健康档案
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const update = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-healify-50 to-green-100 px-4 py-8">
      <div className="card w-full max-w-md">
        <div className="text-center mb-6">
          <h1 className="text-3xl font-bold text-healify-700">💚 Healify</h1>
          <p className="text-gray-500 mt-2">创建你的健康账户</p>
        </div>

        {error && (
          <div className="bg-red-50 text-red-600 px-4 py-3 rounded-lg mb-4 text-sm">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-3">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">用户名 *</label>
            <input type="text" className="input-field" placeholder="3-50个字符"
                   value={form.username} onChange={update('username')} required minLength={3} />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">邮箱 *</label>
            <input type="email" className="input-field" placeholder="your@email.com"
                   value={form.email} onChange={update('email')} required />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">昵称</label>
            <input type="text" className="input-field" placeholder="怎么称呼你？"
                   value={form.nickname} onChange={update('nickname')} />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">密码 *</label>
            <input type="password" className="input-field" placeholder="至少6位"
                   value={form.password} onChange={update('password')} required minLength={6} />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">确认密码 *</label>
            <input type="password" className="input-field" placeholder="再次输入密码"
                   value={form.confirmPassword} onChange={update('confirmPassword')} required />
          </div>

          <button type="submit" disabled={loading} className="btn-primary w-full mt-2">
            {loading ? '注册中...' : '注 册'}
          </button>
        </form>

        <p className="text-center text-sm text-gray-500 mt-4">
          已有账号？
          <Link to="/login" className="text-healify-600 hover:underline ml-1">去登录</Link>
        </p>
      </div>
    </div>
  );
}
