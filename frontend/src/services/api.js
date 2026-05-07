import axios from 'axios';

const api = axios.create({
  baseURL:process.env.REACT_APP_API_URL || 'http://localhost:9090',
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (res) => res,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/';
    }
    return Promise.reject(error);
  }
);

export const login = (email, password) =>
  api.post('/api/auth/login', { email, password });

export default api;