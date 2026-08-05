import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../services/api';
import { useAuth } from '../../context/AuthContext';

export default function MyGrades() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [grades,  setGrades]  = useState([]);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState('');
  const [selectedSemester, setSelectedSemester] = useState('');

  useEffect(() => {
    const load = async () => {
      try {
        const studentsRes = await api.get('/api/students');
        const me = studentsRes.data.find((s) => s.usuarioId === user.id);
        if (!me) {
          setError('No se encontró tu perfil de estudiante.');
          return;
        }
        const gradesRes = await api.get(`/api/grades/student/${me.id}`);
        setGrades(gradesRes.data);
      } catch {
        setError('Error al cargar tus notas.');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [user.id]);

  const semesterGroups = Object.entries(
    grades.reduce((acc, grade) => {
      const key = grade.semesterId
        ? `${grade.semesterYear} — ${grade.semesterPeriod}`
        : 'Sin semestre asignado';
      if (!acc[key]) acc[key] = [];
      acc[key].push(grade);
      return acc;
    }, {})
  ).sort(([a], [b]) => {
    if (a === 'Sin semestre asignado') return 1;
    if (b === 'Sin semestre asignado') return -1;
    return b.localeCompare(a);
  });

  const selectedSemesterGroup =
    semesterGroups.find(([label]) => label === selectedSemester) ?? semesterGroups[0];
  const activeSemesterLabel = selectedSemesterGroup?.[0] ?? '';
  const activeSemesterGrades = selectedSemesterGroup?.[1] ?? [];

  if (loading) return <p className="text-muted p-4">Cargando notas...</p>;
  if (error)   return <div className="alert alert-danger m-4">{error}</div>;

  return (
    <div className="container py-4">
      <div className="mb-4">
        <button className="btn btn-outline-secondary mb-3" onClick={() => navigate(-1)}>
          ← Volver
        </button>
        <h2 className="fw-bold mb-0">Mis Notas</h2>
        <p className="text-muted mb-0">Historial de calificaciones por asignatura</p>
      </div>

      {grades.length === 0 ? (
        <div className="alert alert-info">No tienes notas registradas aún.</div>
      ) : (
        <>
          <div className="card shadow-sm border-0 mb-3">
            <div className="card-body d-md-flex align-items-center gap-4 py-3">
              <div className="mb-3 mb-md-0">
                <div className="fw-semibold">Consultar semestre</div>
                <div className="small text-muted">
                  Revisa tus calificaciones organizadas por período académico.
                </div>
              </div>
              <div className="ms-md-auto" style={{ minWidth: '280px' }}>
                <label className="visually-hidden" htmlFor="grade-semester-select">
                  Seleccionar semestre
                </label>
                <select
                  id="grade-semester-select"
                  className="form-select"
                  value={activeSemesterLabel}
                  onChange={(event) => setSelectedSemester(event.target.value)}
                >
                  {semesterGroups.map(([semesterLabel, items], index) => (
                    <option key={semesterLabel} value={semesterLabel}>
                      {index === 0 ? 'Más reciente — ' : ''}
                      {semesterLabel} ({items.length} {items.length === 1 ? 'nota' : 'notas'})
                    </option>
                  ))}
                </select>
              </div>
            </div>
          </div>

          <h6 className="fw-semibold text-muted mb-2">{activeSemesterLabel}</h6>
          <div className="card shadow-sm border-0">
            <div className="table-responsive">
              <table className="table table-hover mb-0 align-middle">
                <thead className="table-light">
                  <tr>
                    <th>Código</th>
                    <th>Asignatura</th>
                    <th>Nota</th>
                    <th>Fecha</th>
                    <th>Resultado</th>
                  </tr>
                </thead>
                <tbody>
                  {activeSemesterGrades.map((g) => (
                    <tr key={g.gradeId}>
                      <td>
                        <span className="badge bg-primary font-monospace">{g.subjectCode}</span>
                      </td>
                      <td className="fw-semibold">{g.subjectName}</td>
                      <td>
                        <span
                          className={`fw-bold fs-5 ${
                            g.value >= 4.0 ? 'text-success' : 'text-danger'
                          }`}
                        >
                          {Number(g.value).toFixed(1)}
                        </span>
                      </td>
                      <td className="text-muted small">{g.entryDate}</td>
                      <td>
                        <span className={`badge ${g.value >= 4.0 ? 'bg-success' : 'bg-danger'}`}>
                          {g.value >= 4.0 ? 'Aprobado' : 'Reprobado'}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
