import React, { useCallback, useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import api from "../../services/api";

const DAY_NAMES = ['Dom', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb'];

const STATUS_OPTIONS = [
  { value: 'ACTIVE',    label: 'En cursado'  },
  { value: 'COMPLETED', label: 'Completada'  },
];

const statusBadge = (status) => {
  if (status === 'ACTIVE')    return <span className="badge bg-success">En cursado</span>;
  if (status === 'COMPLETED') return <span className="badge bg-primary">Completada</span>;
  if (status === 'CANCELLED') return <span className="badge bg-secondary">Cancelada</span>;
  return <span className="badge bg-secondary">{status}</span>;
};

export default function StudentEnrollmentsAdmin() {
  const { id }   = useParams();
  const navigate = useNavigate();

  const [student,       setStudent]       = useState(null);
  const [enrollments,   setEnrollments]   = useState([]);
  const [sections,      setSections]      = useState([]);
  const [subjects,      setSubjects]      = useState([]);
  const [activeSemId,   setActiveSemId]   = useState(null); // ID del semestre activo
  const [loading,       setLoading]       = useState(true);
  const [error,         setError]         = useState('');

  // Modal nota
  const [gradeModal,    setGradeModal]    = useState(null);
  const [gradeValue,    setGradeValue]    = useState('');
  const [gradeLoading,  setGradeLoading]  = useState(false);
  const [gradeError,    setGradeError]    = useState('');

  // Modal mover
  const [moveModal,     setMoveModal]     = useState(null);
  const [newSectionId,  setNewSectionId]  = useState('');
  const [moveLoading,   setMoveLoading]   = useState(false);
  const [moveError,     setMoveError]     = useState('');

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const [stuRes, enrRes, secRes, subRes, semRes] = await Promise.all([
        api.get(`/api/students/${id}`),
        api.get(`/api/enrollments/student/${id}`),
        api.get('/api/sections'),
        api.get('/api/subjects'),
        api.get('/api/semesters'),
      ]);
      setStudent(stuRes.data);
      setEnrollments(enrRes.data.filter((e) => e.status !== 'CANCELLED'));
      setSections(secRes.data);
      setSubjects(subRes.data);

      // Guardar ID del semestre activo para filtrar secciones al mover
      const active = semRes.data.find((s) => s.status === 'IN_PROGRESS');
      setActiveSemId(active?.id ?? null);
    } catch {
      setError('Error al cargar la información del estudiante.');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => { loadData(); }, [loadData]);

  const getSection  = (sid) => sections.find((s) => s.id === sid);
  const subjectName = (subId) => subjects.find((s) => s.id === subId)?.name ?? `Asignatura #${subId}`;
  const subjectCode = (subId) => subjects.find((s) => s.id === subId)?.code ?? '---';

  const handleStatusChange = async (enrollId, newStatus) => {
    try {
      await api.patch(
        `/api/enrollments/${enrollId}/status`,
        JSON.stringify(newStatus),
        { headers: { 'Content-Type': 'application/json' } }
      );
      loadData();
    } catch { alert('Error al actualizar el estado.'); }
  };

  const handleCancel = async (enrollId, subName) => {
    if (!window.confirm(`¿Cancelar inscripción en "${subName}"? Se devolverá el cupo.`)) return;
    try {
      await api.delete(`/api/enrollments/${enrollId}`);
      loadData();
    } catch { alert('Error al cancelar la inscripción.'); }
  };

  const handleGrade = async () => {
    const val = parseFloat(gradeValue);
    if (isNaN(val) || val < 1 || val > 7) {
      setGradeError('La nota debe estar entre 1.0 y 7.0'); return;
    }
    setGradeLoading(true); setGradeError('');
    try {
      await api.post('/api/grades', {
        enrollmentId: gradeModal.enrollId,
        value: val,
        entryDate: new Date().toISOString().split('T')[0],
      });
      setGradeModal(null); setGradeValue(''); loadData();
    } catch (err) {
      setGradeError(err.response?.data?.message || 'Error al guardar la nota.');
    } finally { setGradeLoading(false); }
  };

  // Solo secciones de la misma asignatura, con cupos, del semestre ACTIVO
  const sectionsForMove = moveModal
    ? sections.filter((s) =>
        s.subjectId       === moveModal.subjectId &&
        s.id              !== moveModal.currentSectionId &&
        s.availableSeats  >  0 &&
        s.semesterId      === activeSemId   // ← solo semestre activo
      )
    : [];

  const handleMove = async () => {
    if (!newSectionId) { setMoveError('Selecciona una sección destino.'); return; }
    setMoveLoading(true); setMoveError('');
    try {
      await api.delete(`/api/enrollments/${moveModal.enrollId}`);
      await api.post('/api/enrollments/enroll', {
        studentId: Number(id),
        sectionId: newSectionId,
      });
      setMoveModal(null); setNewSectionId(''); loadData();
    } catch (err) {
      setMoveError(err.response?.data || 'Error al mover la inscripción.');
    } finally { setMoveLoading(false); }
  };

  if (loading) return <p className="text-muted p-4">Cargando...</p>;

  return (
    <div className="container py-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div className="d-flex align-items-center gap-3">
          <button className="btn btn-outline-secondary" onClick={() => navigate('/students')}>
            ← Volver
          </button>
          <div>
            <h2 className="fw-bold mb-0">Cursos en Cursado</h2>
            <p className="text-muted mb-0 small">
              {student
                ? `${student.firstName} ${student.lastName} · ${student.enrollmentNumber}`
                : `Estudiante #${id}`}
            </p>
          </div>
        </div>
      </div>

      {error && <div className="alert alert-danger">{error}</div>}

      {enrollments.length === 0 && (
        <div className="alert alert-info">Este estudiante no tiene inscripciones activas.</div>
      )}

      {enrollments.length > 0 && (
        <div className="card shadow-sm border-0">
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead className="table-light">
                <tr>
                  <th>Asignatura</th>
                  <th>Sala / Horario</th>
                  <th>Estado</th>
                  <th>Cambiar estado</th>
                  <th className="text-end">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {enrollments.map((e) => {
                  const sec   = getSection(e.sectionId);
                  const subId = sec?.subjectId;
                  return (
                    <tr key={e.id}>
                      <td>
                        <span className="badge bg-primary font-monospace me-1">{subjectCode(subId)}</span>
                        <span className="fw-semibold small">{subjectName(subId)}</span>
                        <div className="text-muted" style={{ fontSize: '0.72rem' }}>Sección #{e.sectionId}</div>
                      </td>
                      <td className="small text-muted">
                        {sec?.room?.name ?? '—'}
                        {sec?.dayOfWeek != null && (
                          <div style={{ fontSize: '0.72rem' }}>
                            {DAY_NAMES[sec.dayOfWeek]} {sec.startTime?.slice(0,5)}–{sec.endTime?.slice(0,5)}
                          </div>
                        )}
                      </td>
                      <td>{statusBadge(e.status)}</td>
                      <td>
                        <select className="form-select form-select-sm" style={{ width: 140 }}
                          value={e.status}
                          onChange={(ev) => handleStatusChange(e.id, ev.target.value)}>
                          {STATUS_OPTIONS.map((opt) => (
                            <option key={opt.value} value={opt.value}>{opt.label}</option>
                          ))}
                        </select>
                      </td>
                      <td className="text-end d-flex gap-1 justify-content-end">
                        <button className="btn btn-sm btn-outline-secondary"
                          onClick={() => {
                            setGradeModal({ enrollId: e.id, subjectName: subjectName(subId) });
                            setGradeValue(''); setGradeError('');
                          }}>📝 Nota</button>
                        <button className="btn btn-sm btn-outline-primary"
                          onClick={() => {
                            setMoveModal({ enrollId: e.id, subjectId: subId, currentSectionId: e.sectionId });
                            setNewSectionId(''); setMoveError('');
                          }}>🔀 Mover</button>
                        <button className="btn btn-sm btn-outline-danger"
                          onClick={() => handleCancel(e.id, subjectName(subId))}>
                          ✕ Cancelar
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Modal Nota */}
      {gradeModal && (
        <div className="modal d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
          <div className="modal-dialog">
            <div className="modal-content">
              <div className="modal-header">
                <h5 className="modal-title">Registrar nota — {gradeModal.subjectName}</h5>
                <button className="btn-close" onClick={() => setGradeModal(null)} />
              </div>
              <div className="modal-body">
                {gradeError && <div className="alert alert-danger py-2 small">{gradeError}</div>}
                <label className="form-label fw-semibold">Nota (1.0 – 7.0)</label>
                <input type="number" className="form-control form-control-lg"
                  min="1.0" max="7.0" step="0.1"
                  value={gradeValue} onChange={(e) => setGradeValue(e.target.value)} />
                {gradeValue && !isNaN(parseFloat(gradeValue)) && (
                  <div className={`alert mt-2 py-1 small ${parseFloat(gradeValue) >= 4 ? 'alert-success' : 'alert-danger'}`}>
                    {parseFloat(gradeValue) >= 4 ? '✅ Aprobado' : '❌ Reprobado'}
                  </div>
                )}
              </div>
              <div className="modal-footer">
                <button className="btn btn-outline-secondary" onClick={() => setGradeModal(null)}>Cancelar</button>
                <button className="btn btn-primary" onClick={handleGrade} disabled={gradeLoading}>
                  {gradeLoading ? 'Guardando...' : 'Guardar nota'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Modal Mover */}
      {moveModal && (
        <div className="modal d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
          <div className="modal-dialog">
            <div className="modal-content">
              <div className="modal-header">
                <h5 className="modal-title">Mover a otra sección</h5>
                <button className="btn-close" onClick={() => setMoveModal(null)} />
              </div>
              <div className="modal-body">
                {moveError && <div className="alert alert-danger py-2 small">{moveError}</div>}
                <p className="small text-muted mb-2">
                  Secciones del <strong>semestre activo</strong> con cupos disponibles:
                </p>
                {sectionsForMove.length === 0 ? (
                  <div className="alert alert-warning py-2 small">
                    No hay otras secciones disponibles en el semestre activo para esta asignatura.
                  </div>
                ) : (
                  <select className="form-select" value={newSectionId}
                    onChange={(e) => setNewSectionId(e.target.value)}>
                    <option value="">— Selecciona sección destino —</option>
                    {sectionsForMove.map((s) => (
                      <option key={s.id} value={s.id}>
                        Sección #{s.id} · {DAY_NAMES[s.dayOfWeek ?? 0]} {s.startTime?.slice(0,5)}–{s.endTime?.slice(0,5)} · {s.room?.name ?? '—'} · {s.availableSeats} cupos
                      </option>
                    ))}
                  </select>
                )}
              </div>
              <div className="modal-footer">
                <button className="btn btn-outline-secondary" onClick={() => setMoveModal(null)}>Cancelar</button>
                <button className="btn btn-primary" onClick={handleMove}
                  disabled={moveLoading || sectionsForMove.length === 0}>
                  {moveLoading ? 'Moviendo...' : 'Confirmar'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
