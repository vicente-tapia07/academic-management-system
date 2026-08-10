import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';

const DAY_NAMES = ['Dom', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb'];

export default function EnrollForm() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [studentId,     setStudentId]     = useState(null);
  const [activeSemester,setActiveSemester]= useState(null);
  const [subjects,      setSubjects]      = useState([]);       // asignaturas disponibles para inscribir
  const [sections,      setSections]      = useState([]);       // todas las secciones del semestre activo
  const [selectedSubjectId, setSelectedSubjectId] = useState('');
  const [skippedCount,  setSkippedCount]  = useState(0);
  const [loading,       setLoading]       = useState(true);
  const [error,         setError]         = useState('');
  const [success,       setSuccess]       = useState('');
  const [enrollingId,   setEnrollingId]   = useState(null);

  // ── Carga inicial: estudiante, semestre activo, malla, inscripciones ──
  useEffect(() => {
    const init = async () => {
      try {
        const studRes = await api.get('/api/students');
        const me = studRes.data.find((s) => s.usuarioId === user.id);
        if (!me) throw new Error('Estudiante no encontrado');
        setStudentId(me.id);

        const semRes = await api.get('/api/semesters');
        const active = semRes.data.find((s) => s.status === 'IN_PROGRESS');
        if (!active) {
          setError('No hay un semestre activo. No es posible inscribirse.');
          setLoading(false);
          return;
        }
        setActiveSemester(active);

        const [allSubjectsRes, curriculumRes, enrollRes, allSectionsRes] = await Promise.all([
          api.get('/api/subjects'),
          api.get(`/api/students/${me.id}/curriculum`),
          api.get(`/api/enrollments/student/${me.id}`),
          api.get('/api/sections'),
        ]);

        // Asignaturas ya aprobadas
        const approvedSubjectIds = new Set(
          curriculumRes.data.filter((c) => c.status === 'APPROVED').map((c) => c.subjectId)
        );

        // Asignaturas con inscripción activa/completada en este semestre
        const activeEnrollSectionIds = new Set(
          enrollRes.data
            .filter((e) => e.status === 'ACTIVE' || e.status === 'COMPLETED')
            .map((e) => e.sectionId)
        );
        const enrolledSubjectIds = new Set(
          allSectionsRes.data
            .filter((s) => activeEnrollSectionIds.has(s.id))
            .map((s) => s.subjectId)
        );

        // Asignaturas que tienen al menos una sección con cupo en el semestre activo
        const subjectIdsWithActiveSection = new Set(
          allSectionsRes.data
            .filter((s) => s.semesterId === active.id && s.availableSeats > 0)
            .map((s) => s.subjectId)
        );

        let skipped = 0;
        const disponibles = allSubjectsRes.data.filter((sub) => {
          if (!subjectIdsWithActiveSection.has(sub.id)) return false; // sin secciones activas, ni se muestra
          if (approvedSubjectIds.has(sub.id))  { skipped++; return false; }
          if (enrolledSubjectIds.has(sub.id))  { skipped++; return false; }
          return true;
        });

        setSkippedCount(skipped);
        setSubjects(disponibles);
        setSections(allSectionsRes.data);
        if (disponibles.length > 0) setSelectedSubjectId(String(disponibles[0].id));
      } catch {
        setError('No se pudieron cargar los datos de inscripción.');
      } finally {
        setLoading(false);
      }
    };
    init();
  }, [user.id]);

  // Secciones disponibles de la asignatura elegida (semestre activo + cupos)
  const visibleSections = selectedSubjectId
    ? sections.filter((s) =>
        s.subjectId      === selectedSubjectId &&
        s.semesterId     === activeSemester?.id &&
        s.availableSeats > 0
      )
    : [];

  const handleSubjectChange = (e) => {
    setSelectedSubjectId(e.target.value);
    setError(''); setSuccess('');
  };

  const handleEnroll = async (sectionId) => {
    if (!studentId || !sectionId) return;
    setEnrollingId(sectionId);
    setError(''); setSuccess('');
    try {
      await api.post('/api/enrollments/enroll', {
        studentId,
        sectionId,
      });
      setSuccess('¡Inscripción exitosa!');
      setTimeout(() => navigate('/my-enrollments'), 1500);
    } catch (err) {
      const msg = (err.response?.data?.message ?? err.response?.data ?? '').toString().toLowerCase();
      if (msg.includes('prerequisit') || msg.includes('requisito')) {
        setError('No cumples los prerrequisitos para esta asignatura.');
      } else if (msg.includes('cupo') || msg.includes('slot') || msg.includes('seat')) {
        setError('No hay cupos disponibles en esta sección.');
      } else if (err.response?.status === 409) {
        setError('Ya estás inscrito en esta asignatura.');
      } else {
        setError(err.response?.data?.toString() || 'No se pudo completar la inscripción.');
      }
    } finally {
      setEnrollingId(null);
    }
  };

  const selectedSubject = subjects.find((s) => String(s.id) === String(selectedSubjectId));

  return (
    <div className="container py-4" style={{ maxWidth: 720 }}>
      <div className="d-flex align-items-center gap-3 mb-4">
        <button className="btn btn-sm btn-outline-secondary" onClick={() => navigate('/my-enrollments')}>
          ← Volver
        </button>
        <div>
          <h2 className="fw-bold mb-0">Inscribir Asignatura</h2>
          {activeSemester && (
            <p className="text-muted mb-0 small">
              Semestre activo: <strong>{activeSemester.year} — {activeSemester.period}</strong>
            </p>
          )}
        </div>
      </div>

      {loading ? (
        <div className="text-center py-5">
          <div className="spinner-border text-primary" role="status" />
          <p className="text-muted mt-2">Cargando datos de inscripción...</p>
        </div>
      ) : (
        <div className="card border-0 shadow-sm">
          <div className="card-body p-4">

            {error   && <div className="alert alert-danger  py-2">{error}</div>}
            {success && <div className="alert alert-success py-2">{success}</div>}

            {skippedCount > 0 && (
              <div className="alert alert-info py-2 small mb-3">
                ℹ️ Se ocultaron <strong>{skippedCount}</strong> asignatura(s) que ya aprobaste
                o en las que ya estás inscrito.
              </div>
            )}

            {/* Selector de asignatura */}
            <div className="mb-3">
              <label className="form-label fw-semibold">Asignatura</label>
              {subjects.length === 0 ? (
                <div className="alert alert-warning py-2 mb-0">
                  No hay asignaturas disponibles para inscribir en el semestre activo.
                </div>
              ) : (
                <select className="form-select" value={selectedSubjectId} onChange={handleSubjectChange}>
                  {subjects.map((sub) => (
                    <option key={sub.id} value={sub.id}>{sub.code} — {sub.name}</option>
                  ))}
                </select>
              )}
            </div>

            {subjects.length > 0 && (
              <>
                {visibleSections.length === 0 ? (
                  <div className="alert alert-info py-2 small">
                    No hay secciones disponibles con cupo para esta asignatura en el semestre activo.
                  </div>
                ) : (
                  <div className="list-group mb-3">
                    {visibleSections.map((s) => (
                      <div key={s.id}
                        className="list-group-item d-flex justify-content-between align-items-center flex-wrap gap-2">
                        <div>
                          <div className="fw-semibold small">
                            {selectedSubject?.code} — {selectedSubject?.name}
                          </div>
                          <div className="text-muted small">
                            🚪 {s.room?.name ?? '—'}{s.room?.building ? ` (${s.room.building})` : ''} · Sección #{s.id}
                          </div>
                          <div className="text-muted small">
                            🗓️ {s.dayOfWeek != null ? DAY_NAMES[s.dayOfWeek] : '—'}
                            {s.startTime ? ` ${s.startTime.slice(0,5)}–${s.endTime?.slice(0,5)}` : ''}
                            {' '}· 👥 {s.availableSeats} cupos
                          </div>
                        </div>
                        <button
                          className="btn btn-primary btn-sm"
                          onClick={() => handleEnroll(s.id)}
                          disabled={enrollingId === s.id}>
                          {enrollingId === s.id ? 'Inscribiendo...' : 'Inscribir'}
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </>
            )}

          </div>
        </div>
      )}
    </div>
  );
}
