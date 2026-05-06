import React from 'react';
import { Link } from 'react-router-dom';

const cards = [
  { to: '/careers',   icon: '🎓', title: 'Carreras',    desc: 'Crear, editar y eliminar carreras universitarias' },
  { to: '/subjects',  icon: '📚', title: 'Asignaturas', desc: 'Gestionar asignaturas y sus créditos' },
  { to: '/sections',  icon: '🏫', title: 'Secciones',   desc: 'Administrar secciones por semestre' },
  { to: '/semesters', icon: '📅', title: 'Semestres',   desc: 'Gestionar semestres y cierre académico' },
  { to: '/students',  icon: '👥', title: 'Estudiantes', desc: 'Listado y estado académico de alumnos' },
  { to: '/reports',   icon: '📊', title: 'Reportes',    desc: 'Tasa de reprobación histórica por asignatura' },
];

export default function AdminDashboard() {
  return (
    <div className="container py-4">
      <div className="mb-4">
        <h2 className="fw-bold">Panel de Administración</h2>
        <p className="text-muted">Sistema de Administración Académica — USACH</p>
      </div>
      <div className="row g-3">
        {cards.map((c) => (
          <div key={c.to} className="col-12 col-sm-6 col-lg-4">
            <Link to={c.to} className="text-decoration-none">
              <div className="card h-100 shadow-sm border-0">
                <div className="card-body d-flex align-items-start gap-3 p-4">
                  <span style={{ fontSize: '2rem' }}>{c.icon}</span>
                  <div>
                    <h5 className="card-title mb-1 fw-semibold">{c.title}</h5>
                    <p className="card-text text-muted small mb-0">{c.desc}</p>
                  </div>
                </div>
              </div>
            </Link>
          </div>
        ))}
      </div>
    </div>
  );
}