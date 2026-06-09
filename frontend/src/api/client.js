import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
});

// 请求拦截器 — 自动附加 Token
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 响应拦截器 — 统一错误处理
api.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    const message = error.response?.data?.error || error.message || '请求失败';
    return Promise.reject(new Error(message));
  }
);

// ===== Auth =====
export const authAPI = {
  login: (data) => api.post('/auth/login', data),
  register: (data) => api.post('/auth/register', data),
};

// ===== Health Profile =====
export const profileAPI = {
  get: () => api.get('/profile'),
  save: (data) => api.put('/profile', data),
};

// ===== Plans =====
export const planAPI = {
  generate: () => api.post('/plans/generate'),
  getCurrent: () => api.get('/plans/current'),
  optimize: () => api.post('/plans/optimize'),
  getHistory: () => api.get('/plans/history'),
};

// ===== Weight Records =====
export const weightAPI = {
  record: (data) => api.post('/weight', data),
  getHistory: () => api.get('/weight/history'),
  recordAndOptimize: (data) => api.post('/weight/record-and-optimize', data),
  checkThisWeek: () => api.get('/weight/check-this-week'),
  getTrendAnalysis: () => api.get('/plans/trend-analysis'),
};

export default api;
