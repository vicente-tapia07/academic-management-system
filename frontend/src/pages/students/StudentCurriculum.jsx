import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../../services/api';
import { useAuth } from '../../context/AuthContext';

const statusConfig = {
  APPROVED: { cls: 'bg-success', label: 'Aprobada', icon: '✅' },
  FAILED: { cls: 'bg-danger', label: 'Reprobada', icon: '❌' },
  PENDING: { cls: 'bg-secondary', label: 'Pendiente', icon: '⏳' },
  ENROLLED: { cls: 'bg-warning text-dark', label: 'Cursando', icon: '📘' },
};
const getStatus = (s) => statusConfig[s] ?? { cls: 'bg-secondary', label: s, icon: '❓' };

export default function StudentCurriculum() {
  const { id } = useParams();
  const { user, isAdmin } = useAuth();
  const navigate = useNavigate();

  const studentId = id ?? user?.id;

  const [curriculum, setCurriculum] = useState([]);
  const [student, setStudent] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selectedSemester, setSelectedSemester] = useState('');

  useEffect(() => {
    if (!studentId) return;
    setSelectedSemester('');
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

  const approved = curriculum.filter((c) => c.status === 'APPROVED').length;
  const failed = curriculum.filter((c) => c.status === 'FAILED').length;
  const enrolled = curriculum.filter((c) => c.status === 'ENROLLED').length;
  const totalCredits = curriculum
    .filter((c) => c.status === 'APPROVED')
    .reduce((acc, c) => acc + (c.credits ?? 0), 0);

  const semesterGroups = Object.entries(
    curriculum.reduce((acc, c) => {
      const key = c.semesterId
        ? `${c.semesterYear} — ${c.semesterPeriod}`
        : 'Sin semestre asignado';
      if (!acc[key]) acc[key] = [];
      acc[key].push(c);
      return acc;
    }, {})
  ).sort(([a], [b]) => {
    if (a === 'Sin semestre asignado') return 1;
    if (b === 'Sin semestre asignado') return -1;
    return b.localeCompare(a);
  });

  const currentSemesterGroup =
    semesterGroups.find(([, items]) => items.some((c) => c.status === 'ENROLLED')) ??
    semesterGroups[0];
  const selectedSemesterGroup =
    semesterGroups.find(([label]) => label === selectedSemester) ?? currentSemesterGroup;
  const activeSemesterLabel = selectedSemesterGroup?.[0] ?? '';
  const activeSemesterItems = selectedSemesterGroup?.[1] ?? [];

  return (
    <div className="container py-4">
      <div className="d-flex align-items-center gap-3 mb-4">
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
      {error && <div className="alert alert-danger">{error}</div>}

      {!loading && !error && (
        <>
          <div className="row g-3 mb-4">
            {[
              { label: 'Aprobadas', value: approved, color: 'success' },
              { label: 'Reprobadas', value: failed, color: 'danger' },
              { label: 'Cursando', value: enrolled, color: 'warning' },
              { label: 'Créditos', value: totalCredits, color: 'primary' },
            ].map((stat) => (
              <div key={stat.label} className="col-6 col-sm-3">
                <div
                  className={`card border-0 bg-${stat.color} bg-opacity-10 text-center py-3`}
                >
                  <div className={`fs-3 fw-bold text-${stat.color}`}>{stat.value}</div>
                  <div className="small text-muted">{stat.label}</div>
                </div>
              </div>
            ))}
          </div>

          {semesterGroups.length === 0 ? (
            <div className="card shadow-sm border-0">
              <div className="card-body text-center text-muted py-4">
                No hay asignaturas en la malla
              </div>
            </div>
          ) : (
            <>
              <div className="card shadow-sm border-0 mb-3">
                <div className="card-body d-md-flex align-items-center gap-4 py-3">
                  <div className="mb-3 mb-md-0">
                    <div className="fw-semibold">Consultar semestre</div>
                    <div className="small text-muted">
                      Revisa tus asignaturas actuales o consulta semestres anteriores.
                    </div>
                  </div>
                  <div className="ms-md-auto" style={{ minWidth: '280px' }}>
                    <label className="visually-hidden" htmlFor="semester-select">
                      Seleccionar semestre
                    </label>
                    <select
                      id="semester-select"
                      className="form-select"
                      value={activeSemesterLabel}
                      onChange={(event) => setSelectedSemester(event.target.value)}
                    >
                      {semesterGroups.map(([semesterLabel, items]) => (
                        <option key={semesterLabel} value={semesterLabel}>
                          {semesterLabel === currentSemesterGroup?.[0] ? 'Actual — ' : ''}
                          {semesterLabel} ({items.length}{' '}
                          {items.length === 1 ? 'asignatura' : 'asignaturas'})
                        </option>
                      ))}
                    </select>
                  </div>
                </div>
              </div>

              <div className="mb-4">
                <h6 className="fw-semibold text-muted mb-2">{activeSemesterLabel}</h6>
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
                        {activeSemesterItems.map((c) => {
                          const cfg = getStatus(c.status);
                          return (
                            <tr key={c.subjectId}>
                              <td>
                                <span className="badge bg-primary font-monospace">
                                  {c.subjectCode}
                                </span>
                              </td>
                              <td className="fw-semibold">{c.subjectName}</td>
                              <td className="text-muted">{c.credits ?? '—'}</td>
                              <td>
                                {c.grade != null ? (
                                  <span
                                    className={`fw-bold ${
                                      c.grade >= 4 ? 'text-success' : 'text-danger'
                                    }`}
                                  >
                                    {Number(c.grade).toFixed(1)}
                                  </span>
                                ) : (
                                  <span className="text-muted">—</span>
                                )}
                              </td>
                              <td>
                                <span className={`badge ${cfg.cls}`}>
                                  {cfg.icon} {cfg.label}
                                </span>
                              </td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            </>
          )}
        </>
      )}
    </div>
  );
}
