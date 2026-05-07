import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';

export default function StudentEnrollments() {
  const { user }                      = useAuth();
  const navigate                      = useNavigate();
  const [enrollments, setEnrollments] = useState([]);
  const [loading,     setLoading]     = useState(true);
  const [error,       setError]       = useState('');
  const [cancelling,  setCancelling]  = useState(null); // ID de la inscripción que se está cancelando

  const fetchData = async () => {
    try {
      setLoading(true);
      const enrollRes = await api.get(`/api/enrollments/student/${user.id}`);
      const raw = enrollRes.data;

      const enriched = await Promise.all(
        raw.map(async (e) => {
          try {
            const sectionRes = await api.get(`/api/sections/${e.sectionId}`);
            const section = sectionRes.data;

            let subjectName = '—';
            try {
              const subjRes = await api.get(`/api/subjects/${section.subjectId}`);
              subjectName = subjRes.data.name ?? '—';
            } catch { /* mantiene —  */ }

            let professorName = '—';
            try {
              const profRes = await api.get(`/api/professors/${section.professorId}`);
              const p = profRes.data;
              professorName = `${p.firstName} ${p.lastName}`.trim() || '—';
            } catch { /* mantiene — */ }

            return {
              ...e,
              subjectName,
              professorName,
              availableSeats: section.availableSeats,
              totalSeats:     section.totalSeats,
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
  };

  useEffect(() => { fetchData(); }, [user.id]);

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
      alert(err.response?.data?.message || 'Error al cancelar la inscripción.');
    } finally {
      setCancelling(null);
    }
  };

  const visibles = enrollments.filter((e) => e.status !== 'CANCELLED');

  return (
    <div className="container py-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div className="d-flex align-items-center gap-3">
          <button className="btn btn-sm btn-outline-secondary" onClick={() => navigate('/my-dashboard')}>
            ← Volver
          </button>
          <div>
            <h2 className="fw-bold mb-0">Mis Inscripciones</h2>
            <p className="text-muted mb-0 small">Cursos inscritos en el semestre activo</p>
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
          <p className="text-muted small mb-2">{visibles.length} inscripción(es)</p>
          <div className="card shadow-sm border-0">
            <div className="table-responsive">
              <table className="table table-hover mb-0">
                <thead className="table-light">
                  <tr>
                    <th>Asignatura</th>
                    <th>Sección</th>
                    <th>Profesor</th>
                    <th>Cupos</th>
                    <th>Nota</th>
                    <th className="text-end">Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {visibles.length === 0 && (
                    <tr>
                      <td colSpan={6} className="text-center text-muted py-4">
                        No tienes inscripciones activas
                      </td>
                    </tr>
                  )}
                  {visibles.map((e) => {
                    const enrollId = e.id;
                    const tieneNota = e.grade != null;
                    const estaActiva = e.status === 'ACTIVE';

                    return (
                      <tr key={enrollId}>
                        <td className="fw-semibold">{e.subjectName}</td>
                        <td>
                          <span className="badge bg-secondary">
                            {e.sectionCode ?? e.sectionId}
                          </span>
                        </td>
                        <td className="text-muted small">{e.professorName}</td>
                        <td className="text-muted small">
                          {e.availableSeats != null
                            ? `${e.availableSeats} / ${e.totalSeats}`
                            : '—'}
                        </td>
                        <td>
                          {tieneNota
                            ? <span className={`fw-bold ${e.grade >= 4 ? 'text-success' : 'text-danger'}`}>
                                {Number(e.grade).toFixed(1)}
                              </span>
                            : <span className="text-muted">—</span>}
                        </td>
                        <td className="text-end">
                          {/* Solo se puede cancelar si está ACTIVA y no tiene nota */}
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