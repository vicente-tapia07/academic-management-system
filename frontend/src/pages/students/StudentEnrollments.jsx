import React, { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';

export default function StudentEnrollments() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [enrollments, setEnrollments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [cancelling, setCancelling] = useState(null);
  const [statusFilter, setStatusFilter] = useState('ALL');

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      setError('');
      const [enrollRes, gradesRes, semestersRes] = await Promise.all([
        api.get(`/api/enrollments/student/${user.id}`),
        api.get(`/api/grades/student/${user.id}`),
        api.get('/api/semesters'),
      ]);
      const raw = enrollRes.data;
      const grades = gradesRes.data;
      const semesters = semestersRes.data;

      const enriched = await Promise.all(
        raw.map(async (e) => {
          try {
            const sectionRes = await api.get(`/api/sections/${e.sectionId}`);
            const section = sectionRes.data;

            let subjectName = '—';
            try {
              const subjRes = await api.get(`/api/subjects/${section.subjectId}`);
              subjectName = subjRes.data.name ?? '—';
            } catch {
              /* mantiene — */
            }

            let professorName = '—';
            try {
              const profRes = await api.get(`/api/professors/${section.professorId}`);
              const p = profRes.data;
              professorName = `${p.firstName} ${p.lastName}`.trim() || '—';
            } catch {
              /* mantiene — */
            }

            const semester = semesters.find((s) => s.id === section.semesterId);
            const grade = grades.find(
              (g) => g.subjectId === section.subjectId && g.semesterId === section.semesterId
            );

            return {
              ...e,
              subjectName,
              professorName,
              availableSeats: section.availableSeats,
              totalSeats: section.totalSeats,
              semesterLabel: semester ? `${semester.year} — ${semester.period}` : '—',
              semesterYear: semester?.year,
              semesterPeriod: semester?.period,
              grade: grade?.value ?? null,
            };
          } catch {
            return e;
          }
        })
      );

      setEnrollments(enriched);
    } catch {
      setError('No se pudieron cargar las inscripciones.');
    } finally {
      setLoading(false);
    }
  }, [user.id]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleCancel = async (enrollmentId, subjectName) => {
    const confirmar = window.confirm(
      `¿Cancelar inscripción en "${subjectName}"?\nSe devolverá el cupo a la sección.`
    );
    if (!confirmar) return;

    setCancelling(enrollmentId);
    try {
      await api.delete(`/api/enrollments/${enrollmentId}`);
      await fetchData();
    } catch (err) {
      const detail = err.response?.data;
      alert(
        (typeof detail === 'string' ? detail : detail?.error || detail?.message) ||
          'Error al cancelar la inscripción.'
      );
    } finally {
      setCancelling(null);
    }
  };

  const statusOptions = [
    { value: 'ALL', label: 'Todas', color: 'secondary' },
    { value: 'ACTIVE', label: 'Activas', color: 'success' },
    { value: 'COMPLETED', label: 'Completadas', color: 'primary' },
    { value: 'CANCELLED', label: 'Canceladas', color: 'danger' },
  ];

  const statusLabel = (status) => {
    if (status === 'ACTIVE') return { label: 'Activa', cls: 'bg-success' };
    if (status === 'COMPLETED') return { label: 'Completada', cls: 'bg-primary' };
    if (status === 'CANCELLED') return { label: 'Cancelada', cls: 'bg-danger' };
    return { label: status, cls: 'bg-secondary' };
  };

  const counts = enrollments.reduce(
    (acc, enrollment) => ({
      ...acc,
      [enrollment.status]: (acc[enrollment.status] ?? 0) + 1,
    }),
    {}
  );
  const visibles = statusFilter === 'ALL'
    ? enrollments
    : enrollments.filter((e) => e.status === statusFilter);

  return (
    <div className="container py-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div className="d-flex align-items-center gap-3">
          <button
            className="btn btn-sm btn-outline-secondary"
            onClick={() => navigate('/my-dashboard')}
          >
            ← Volver
          </button>
          <div>
            <h2 className="fw-bold mb-0">Mis Inscripciones</h2>
            <p className="text-muted mb-0 small">Historial de inscripciones por estado</p>
          </div>
        </div>
        <Link to="/my-enroll" className="btn btn-primary btn-sm">
          + Inscribir asignatura
        </Link>
      </div>

      {loading && (
        <div className="text-center py-5">
          <div className="spinner-border text-primary" role="status" />
          <p className="text-muted mt-2">Cargando inscripciones...</p>
        </div>
      )}
      {error && <div className="alert alert-danger">{error}</div>}

      {!loading && !error && (
        <>
          <div className="d-flex flex-wrap gap-2 mb-3" role="group" aria-label="Filtrar inscripciones">
            {statusOptions.map((option) => {
              const count = option.value === 'ALL'
                ? enrollments.length
                : counts[option.value] ?? 0;
              const selected = statusFilter === option.value;
              return (
                <button
                  key={option.value}
                  type="button"
                  className={`btn btn-sm ${selected ? `btn-${option.color}` : `btn-outline-${option.color}`}`}
                  onClick={() => setStatusFilter(option.value)}
                >
                  {option.label} <span className="badge bg-light text-dark ms-1">{count}</span>
                </button>
              );
            })}
          </div>
          <p className="text-muted small mb-2">{visibles.length} inscripción(es)</p>
          <div className="card shadow-sm border-0">
            <div className="table-responsive">
              <table className="table table-hover mb-0">
                <thead className="table-light">
                  <tr>
                    <th>Asignatura</th>
                    <th>Sección</th>
                    <th>Profesor</th>
                    <th>Semestre</th>
                    <th>Estado</th>
                    <th>Cupos</th>
                    <th>Nota</th>
                    <th className="text-end">Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {visibles.length === 0 && (
                    <tr>
                      <td colSpan={8} className="text-center text-muted py-4">
                        No hay inscripciones en esta categoría
                      </td>
                    </tr>
                  )}
                  {visibles.map((e) => {
                    const enrollId = e.id;
                    const tieneNota = e.grade != null;
                    const estaActiva = e.status === 'ACTIVE';
                    const status = statusLabel(e.status);

                    return (
                      <tr key={enrollId}>
                        <td className="fw-semibold">{e.subjectName}</td>
                        <td>
                          <span className="badge bg-secondary">
                            {e.sectionCode ?? e.sectionId}
                          </span>
                        </td>
                        <td className="text-muted small">{e.professorName}</td>
                        <td className="text-muted small text-nowrap">{e.semesterLabel ?? '—'}</td>
                        <td><span className={`badge ${status.cls}`}>{status.label}</span></td>
                        <td className="text-muted small">
                          {e.availableSeats != null
                            ? `${e.availableSeats} / ${e.totalSeats}`
                            : '—'}
                        </td>
                        <td>
                          {tieneNota ? (
                            <span
                              className={`fw-bold ${e.grade >= 4 ? 'text-success' : 'text-danger'}`}
                            >
                              {Number(e.grade).toFixed(1)}
                            </span>
                          ) : (
                            <span className="text-muted">—</span>
                          )}
                        </td>
                        <td className="text-end">
                          {estaActiva && !tieneNota ? (
                            <button
                              className="btn btn-sm btn-outline-danger"
                              disabled={cancelling === enrollId}
                              onClick={() => handleCancel(enrollId, e.subjectName)}
                            >
                              {cancelling === enrollId ? 'Cancelando...' : 'Cancelar'}
                            </button>
                          ) : (
                            <span className="text-muted small">—</span>
                          )}
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
