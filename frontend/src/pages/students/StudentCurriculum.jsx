import React, { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import api from '../../services/api';
import { useAuth } from '../../context/AuthContext';

const statusConfig = {
  APPROVED: { cls: 'bg-success',           label: 'Aprobada',  icon: '✅' },
  FAILED:   { cls: 'bg-danger',            label: 'Reprobada', icon: '❌' },
  PENDING:  { cls: 'bg-warning text-dark', label: 'Cursando',  icon: '📘' },
  ENROLLED: { cls: 'bg-info text-dark',    label: 'Inscrita',  icon: '📋' },
};
const getStatus = (s) => statusConfig[s] ?? { cls: 'bg-secondary', label: s, icon: '❓' };

export default function StudentCurriculum() {
  const { id }        = useParams();
  const { user, isAdmin } = useAuth();
  const navigate      = useNavigate();

  // Si hay id en la URL (admin ve la malla de otro), lo usa. 
  // Si no (estudiante ve /my-curriculum), usa su propio id del token.
  const studentId = id ?? user?.id;

  const [curriculum, setCurriculum] = useState([]);
  const [student,    setStudent]    = useState(null);
  const [loading,    setLoading]    = useState(true);
  const [error,      setError]      = useState('');

  useEffect(() => {
    if (!studentId) return;
    Promise.all([
      api.get(`/api/students/${studentId}/curriculum`),
      api.get(`/api/students/${studentId}`),
    ])
      .then(([currRes, studRes]) => {
        setCurriculum(currRes.data);
        setStudent(studRes.data);
      })
      .catch(() => setError('No se pudo cargar la malla curricular.'))
      .finally(() => setLoading(false));
  }, [studentId]);

  const approved     = curriculum.filter((c) => c.status === 'APPROVED').length;
  const failed       = curriculum.filter((c) => c.status === 'FAILED').length;
  const pending      = curriculum.filter((c) => ['PENDING', 'ENROLLED'].includes(c.status)).length;
  const totalCredits = curriculum
    .filter((c) => c.status === 'APPROVED')
    .reduce((acc, c) => acc + (c.credits ?? 0), 0);

  return (
    <div className="container py-4">
      <div className="d-flex align-items-center gap-3 mb-4">
        {/* Admin vuelve a /students, estudiante vuelve a su dashboard */}
        <button
          className="btn btn-sm btn-outline-secondary"
          onClick={() => navigate(isAdmin ? '/students' : '/my-dashboard')}
        >
          ← Volver
        </button>
        <div>
          <h2 className="fw-bold mb-0">
            Malla Curricular
            {student && ` — ${student.firstName} ${student.lastName}`}
          </h2>
          {student && (
            <p className="text-muted mb-0 small">
              Matrícula: <code>{student.enrollmentNumber}</code>
            </p>
          )}
        </div>
      </div>

      {loading && <p className="text-muted">Cargando malla...</p>}
      {error   && <div className="alert alert-danger">{error}</div>}

      {!loading && !error && (
        <>
          <div className="row g-3 mb-4">
            {[
              { label: 'Aprobadas',  value: approved,      color: 'success'  },
              { label: 'Reprobadas', value: failed,        color: 'danger'   },
              { label: 'Cursando',   value: pending,       color: 'warning'  },
              { label: 'Créditos',   value: totalCredits,  color: 'primary'  },
            ].map((stat) => (
              <div key={stat.label} className="col-6 col-sm-3">
                <div className={`card border-0 bg-${stat.color} bg-opacity-10 text-center py-3`}>
                  <div className={`fs-3 fw-bold text-${stat.color}`}>{stat.value}</div>
                  <div className="small text-muted">{stat.label}</div>
                </div>
              </div>
            ))}
          </div>

          <div className="card shadow-sm border-0">
            <div className="table-responsive">
              <table className="table table-hover mb-0">
                <thead className="table-light">
                  <tr>
                    <th>Código</th>
                    <th>Asignatura</th>
                    <th>Créditos</th>
                    <th>Nota</th>
                    <th>Estado</th>
                  </tr>
                </thead>
                <tbody>
                  {curriculum.length === 0 && (
                    <tr>
                      <td colSpan={5} className="text-center text-muted py-4">
                        No hay asignaturas en la malla
                      </td>
                    </tr>
                  )}
                  {curriculum.map((c) => {
                    const cfg = getStatus(c.status);
                    return (
                      <tr key={c.subjectId}>
                        <td><span className="badge bg-primary font-monospace">{c.subjectCode}</span></td>
                        <td className="fw-semibold">{c.subjectName}</td>
                        <td className="text-muted">{c.credits ?? '—'}</td>
                        <td>
                          {c.grade != null
                            ? <span className={`fw-bold ${c.grade >= 4 ? 'text-success' : 'text-danger'}`}>{Number(c.grade).toFixed(1)}</span>
                            : <span className="text-muted">—</span>}
                        </td>
                        <td>
                          <span className={`badge ${cfg.cls}`}>{cfg.icon} {cfg.label}</span>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}
    </div>
  );
}