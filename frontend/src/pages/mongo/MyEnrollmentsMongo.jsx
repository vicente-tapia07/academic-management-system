import React, { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';

const DAY_LABELS = ['', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];

const statusBadge = (status) => {
  if (status === 'ACTIVE') return { label: 'Activa', cls: 'bg-success' };
  if (status === 'COMPLETED') return { label: 'Completada', cls: 'bg-primary' };
  if (status === 'CANCELLED') return { label: 'Cancelada', cls: 'bg-danger' };
  return { label: status ?? '—', cls: 'bg-secondary' };
};

const formatDate = (iso) => {
  if (!iso) return '—';
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return iso;
  return date.toLocaleDateString('es-CL', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

export default function MyEnrollmentsMongo() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [enrollments, setEnrollments] = useState([]);
  const [subjectById, setSubjectById] = useState({});
  const [sectionById, setSectionById] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      setError('');
      const studentRes = await api.get(`/api/mongo/students/by-user/${user.id}`);
      const studentId = studentRes.data.id;

      const [enrollmentsRes, subjectsRes] = await Promise.all([
        api.get(`/api/mongo/enrollments/student/${studentId}`),
        api.get('/api/mongo/subjects'),
      ]);

      const byId = (acc, item) => {
        acc[item.id] = item;
        return acc;
      };
      setSubjectById(subjectsRes.data.reduce(byId, {}));

      // Las secciones se resuelven por asignatura+semestre; solo se obtienen las
      // que siguen abiertas con cupo, el resto se muestra con su sectionId.
      const sectionMap = {};
      const semesterIds = new Set(
        enrollmentsRes.data.map((e) => e.semesterId).filter(Boolean)
      );
      for (const subjectId of new Set(enrollmentsRes.data.map((e) => e.subjectId))) {
        for (const semesterId of semesterIds) {
          try {
            const sectionsRes = await api.get('/api/mongo/sections', {
              params: { subjectId, semesterId },
            });
            sectionsRes.data.forEach((s) => {
              sectionMap[s.id] = s;
            });
          } catch {
            /* se mantiene la sección sin enriquecer */
          }
        }
      }
      setSectionById(sectionMap);
      setEnrollments(enrollmentsRes.data);
    } catch (err) {
      const data = err.response?.data;
      setError(data?.error || 'No se pudieron cargar las inscripciones (MongoDB).');
    } finally {
      setLoading(false);
    }
  }, [user.id]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const statusOptions = [
    { value: 'ALL', label: 'Todas', color: 'secondary' },
    { value: 'ACTIVE', label: 'Activas', color: 'success' },
    { value: 'COMPLETED', label: 'Completadas', color: 'primary' },
    { value: 'CANCELLED', label: 'Canceladas', color: 'danger' },
  ];

  const counts = enrollments.reduce(
    (acc, e) => ({ ...acc, [e.status]: (acc[e.status] ?? 0) + 1 }),
    {}
  );
  const visibles =
    statusFilter === 'ALL'
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
            <h2 className="fw-bold mb-0">Mis Inscripciones (MongoDB)</h2>
            <p className="text-muted mb-0 small">
              Laboratorio 3 · Referencias resueltas desde las colecciones MongoDB
            </p>
          </div>
        </div>
        <Link to="/mongo/enroll" className="btn btn-primary btn-sm">
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
              const count =
                option.value === 'ALL'
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
                  {option.label}{' '}
                  <span className="badge bg-light text-dark ms-1">{count}</span>
                </button>
              );
            })}
          </div>

          <p className="text-muted small mb-2">
            {visibles.length} inscripción(es)
          </p>

          <div className="card shadow-sm border-0">
            <div className="table-responsive">
              <table className="table table-hover mb-0 align-middle">
                <thead className="table-light">
                  <tr>
                    <th>Asignatura</th>
                    <th>Sección</th>
                    <th>Profesor</th>
                    <th>Horario</th>
                    <th>Estado</th>
                    <th>Reglas de negocio</th>
                    <th>Inscrito</th>
                    <th>Actualizado</th>
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
                    const subject = subjectById[e.subjectId];
                    const section = sectionById[e.sectionId];
                    const status = statusBadge(e.status);
                    const rules = e.businessRules;
                    return (
                      <tr key={e.id}>
                        <td className="fw-semibold">
                          {subject
                            ? `${subject.code} — ${subject.name}`
                            : e.subjectId}
                        </td>
                        <td>
                          {section ? (
                            <span className="badge bg-secondary">
                              {section.room?.code} · Sección #{section.id.slice(-6)}
                            </span>
                          ) : (
                            <span className="badge bg-secondary">{e.sectionId}</span>
                          )}
                        </td>
                        <td className="text-muted small">
                          {section?.professorName ?? '—'}
                        </td>
                        <td className="text-muted small text-nowrap">
                          {section?.schedule
                            ? `${DAY_LABELS[section.schedule.dayOfWeek] ?? '—'} ${section.schedule.startTime}–${section.schedule.endTime}`
                            : '—'}
                        </td>
                        <td>
                          <span className={`badge ${status.cls}`}>{status.label}</span>
                        </td>
                        <td>
                          <span
                            className={`badge ${rules?.prerequisitesSatisfied ? 'bg-success' : 'bg-danger'} me-1`}
                            title="Prerrequisitos verificados por la transacción"
                          >
                            Pre: {rules?.prerequisitesSatisfied ? 'OK' : 'NO'}
                          </span>
                          <span
                            className={`badge ${rules?.seatAvailableAtEnrollment ? 'bg-success' : 'bg-danger'}`}
                            title="Cupo reservado al inscribir"
                          >
                            Cupo: {rules?.seatAvailableAtEnrollment ? 'OK' : 'NO'}
                          </span>
                        </td>
                        <td className="text-muted small text-nowrap">
                          {formatDate(e.enrolledAt)}
                        </td>
                        <td className="text-muted small text-nowrap">
                          {formatDate(e.updatedAt)}
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
