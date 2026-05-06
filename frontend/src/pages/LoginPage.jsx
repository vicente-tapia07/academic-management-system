import React, { useState } from 'react';
import { useNavigate, Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { login } from '../services/api';

export default function LoginPage() {
  const { user, loginUser } = useAuth();
  const navigate = useNavigate();

  const [form,    setForm]    = useState({ email: '', password: '' });
  const [error,   setError]   = useState('');
  const [loading, setLoading] = useState(false);

  // Si ya está logueado, redirige directo
  if (user) {
    const roles = user.roles ?? [];
    if (roles.some(r => r.includes('ADMIN')))          return <Navigate to="/dashboard"    replace />;
    if (roles.some(r => r.includes('PROFESSOR')))      return <Navigate to="/professor"    replace />;
    return <Navigate to="/my-dashboard" replace />;
  }

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.email || !form.password) {
      setError('Por favor completa todos los campos.');
      return;
    }
    setLoading(true);
    try {
      const res      = await login(form.email, form.password);
      const userData = loginUser(res.data.token);

      // Redirige según rol
      const roles = userData?.roles ?? [];
      if (roles.some(r => r.includes('ADMIN')))          navigate('/dashboard');
      else if (roles.some(r => r.includes('PROFESSOR'))) navigate('/professor');
      else navigate('/my-dashboard');
    } catch (err) {
      const status = err.response?.status;
      if (status === 401 || status === 403) {
        setError('Credenciales incorrectas. Verifica tu email y contraseña.');
      } else {
        setError('No se pudo conectar al servidor. ¿Está corriendo el backend?');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      className="min-vh-100 d-flex align-items-center justify-content-center"
      style={{ background: 'linear-gradient(135deg, #003366 0%, #005599 55%, #0077cc 100%)' }}
    >
      <div className="container">
        <div className="row justify-content-center">
          <div className="col-12 col-sm-8 col-md-6 col-lg-4">

            {/* Header */}
            <div className="text-center mb-4">
              <div style={{ fontSize: '3.5rem', lineHeight: 1 }}>🎓</div>
              <h1 className="text-white fw-bold mt-2 mb-0" style={{ letterSpacing: '-0.5px' }}>
                USACH
              </h1>
              <p className="text-white-50 small mb-0">Sistema de Administración Académica</p>
            </div>

            {/* Card */}
            <div className="card border-0 shadow-lg rounded-4">
              <div className="card-body p-4">
                <h5 className="fw-semibold mb-4" style={{ color: '#003366' }}>
                  Iniciar sesión
                </h5>

                {error && (
                  <div className="alert alert-danger py-2 d-flex align-items-center gap-2">
                    <span>⚠️</span>
                    <small>{error}</small>
                  </div>
                )}

                <form onSubmit={handleSubmit} noValidate>
                  <div className="mb-3">
                    <label className="form-label small fw-medium text-muted">
                      Correo electrónico
                    </label>
                    <input
                      type="email"
                      name="email"
                      className="form-control"
                      placeholder="usuario@usach.cl"
                      value={form.email}
                      onChange={handleChange}
                      autoComplete="email"
                      autoFocus
                    />
                  </div>

                  <div className="mb-4">
                    <label className="form-label small fw-medium text-muted">
                      Contraseña
                    </label>
                    <input
                      type="password"
                      name="password"
                      className="form-control"
                      placeholder="••••••••"
                      value={form.password}
                      onChange={handleChange}
                      autoComplete="current-password"
                    />
                  </div>

                  <button
                    type="submit"
                    className="btn w-100 fw-semibold text-white"
                    style={{ backgroundColor: '#003366' }}
                    disabled={loading}
                  >
                    {loading ? (
                      <>
                        <span className="spinner-border spinner-border-sm me-2" role="status" />
                        Autenticando...
                      </>
                    ) : 'Ingresar'}
                  </button>
                </form>
              </div>
            </div>

            <p className="text-center text-white-50 small mt-3">
              Taller de Base de Datos · DIINF · 2026
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}