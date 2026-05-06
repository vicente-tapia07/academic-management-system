import React from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Navbar() {
  const { user, logout, isAdmin, isProfessor, isStudent } = useAuth();
  const navigate  = useNavigate();
  const location  = useLocation();

  const handleLogout = () => { logout(); navigate('/'); };
  const active = (path) => location.pathname.startsWith(path) ? 'active fw-semibold' : '';

  // Ruta home según rol
  const homeRoute = isAdmin ? '/dashboard' : isStudent ? '/my-curriculum' : '/professor';

  if (!user) return null;

  return (
    <nav className="navbar navbar-expand-lg navbar-dark shadow-sm"
      style={{ backgroundColor: '#003366' }}>
      <div className="container-fluid px-4">

        {/* Logo — lleva al home del rol */}
        <Link className="navbar-brand fw-bold d-flex align-items-center gap-2" to={homeRoute}>
          <span style={{ fontSize: '1.3rem' }}>🎓</span>
          USACH <span className="fw-light ms-1">Académico</span>
        </Link>

        <button className="navbar-toggler" type="button"
          data-bs-toggle="collapse" data-bs-target="#mainNav">
          <span className="navbar-toggler-icon" />
        </button>

        <div className="collapse navbar-collapse" id="mainNav">
          <ul className="navbar-nav me-auto mb-2 mb-lg-0 gap-1">

            {/* ADMIN */}
            {isAdmin && (
              <>
                <li className="nav-item">
                  <Link className={`nav-link ${active('/dashboard')}`} to="/dashboard">📊 Panel</Link>
                </li>
                <li className="nav-item">
                  <Link className={`nav-link ${active('/careers')}`} to="/careers">🎓 Carreras</Link>
                </li>
                <li className="nav-item">
                  <Link className={`nav-link ${active('/subjects')}`} to="/subjects">📚 Asignaturas</Link>
                </li>
                <li className="nav-item">
                  <Link className={`nav-link ${active('/sections')}`} to="/sections">🏫 Secciones</Link>
                </li>
                <li className="nav-item">
                  <Link className={`nav-link ${active('/semesters')}`} to="/semesters">📅 Semestres</Link>
                </li>
                <li className="nav-item">
                  <Link className={`nav-link ${active('/students')}`} to="/students">👤 Estudiantes</Link>
                </li>
                <li className="nav-item">
                  <Link className={`nav-link ${active('/reports')}`} to="/reports">📈 Reportes</Link>
                </li>
              </>
            )}

            {/* PROFESSOR */}
            {isProfessor && (
              <>
                <li className="nav-item">
                  <Link className={`nav-link ${active('/professor')}`} to="/professor">📊 Mi Panel</Link>
                </li>
                <li className="nav-item">
                  <Link className={`nav-link ${active('/reports')}`} to="/reports">📈 Reportes</Link>
                </li>
              </>
            )}

            {/* STUDENT */}
            {isStudent && (
              <>
                <li className="nav-item">
                  <Link className={`nav-link ${active('/my-dashboard')}`} to="/my-dashboard">📊 Inicio</Link>
                </li>
                <li className="nav-item">
                  <Link className={`nav-link ${active('/my-curriculum')}`} to="/my-curriculum">📋 Mi Malla</Link>
                </li>
                <li className="nav-item">
                  <Link className={`nav-link ${active('/my-enrollments')}`} to="/my-enrollments">📚 Inscripciones</Link>
                </li>
                <li className="nav-item">
                  <Link className={`nav-link ${active('/my-profile')}`} to="/my-profile">👤 Mi Perfil</Link>
                </li>
              </>
            )}

          </ul>

          {/* Usuario + logout */}
          <div className="d-flex align-items-center gap-3">
            <span className="text-white-50 small">
              <span className="badge bg-secondary me-1">
                {user.roles?.[0]?.replace('ROLE_', '') ?? 'USER'}
              </span>
              {user.email}
            </span>
            <button className="btn btn-outline-light btn-sm" onClick={handleLogout}>
              Cerrar sesión
            </button>
          </div>
        </div>
      </div>
    </nav>
  );
}