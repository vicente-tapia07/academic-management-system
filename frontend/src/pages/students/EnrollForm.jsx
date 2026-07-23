import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';

const DAY_NAMES = ['Dom', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb'];

export default function EnrollForm() {
  const { user }    = useAuth();
  const navigate    = useNavigate();

  const [sections,      setSections]      = useState([]);
  const [selected,      setSelected]      = useState('');
  const [loading,       setLoading]       = useState(true);
  const [saving,        setSaving]        = useState(false);
  const [error,         setError]         = useState('');
  const [success,       setSuccess]       = useState('');
  const [studentId,     setStudentId]     = useState(null);
  const [semesterLabel, setSemesterLabel] = useState('');
  const [skippedCount,  setSkippedCount]  = useState(0); // cuántas se ocultaron

  useEffect(() => {
    const fetchData = async () => {
      try {
        // 1. Obtener el ID del estudiante
        const studRes = await api.get('/api/students');
        const me = studRes.data.find(s => s.usuarioId === user.id);
        if (!me) throw new Error('Estudiante no encontrado');
        setStudentId(me.id);

        // 2. Semestre activo
        const semRes = await api.get('/api/semesters');
        const activeSem = semRes.data.find(s => s.status === 'IN_PROGRESS');
        if (!activeSem) {
          setError('No hay un semestre activo. No es posible inscribirse.');
          setLoading(false);
          return;
        }
        setSemesterLabel(`${activeSem.year} — ${activeSem.period}`);

        // 3. Obtener en paralelo: secciones, malla y inscripciones actuales
        const [secRes, curriculumRes, enrollRes] = await Promise.all([
          api.get('/api/sections'),
          api.get(`/api/students/${me.id}/curriculum`),
          api.get(`/api/enrollments/student/${me.id}`),
        ]);

        // IDs de asignaturas ya aprobadas
        const approvedSubjectIds = new Set(
          curriculumRes.data
            .filter(c => c.status === 'APPROVED')
            .map(c => c.subjectId)
        );

        // IDs de asignaturas que ya tiene inscritas (ACTIVE o COMPLETED)
        // Para esto necesitamos cruzar enrollment → section → subject
        // Usamos las secciones ya cargadas para obtener el subjectId
        const allSections = secRes.data;
        const activeEnrollSectionIds = new Set(
          enrollRes.data
            .filter(e => e.status === 'ACTIVE' || e.status === 'COMPLETED')
            .map(e => e.sectionId)
        );

        const enrolledSubjectIds = new Set(
          allSections
            .filter(s => activeEnrollSectionIds.has(s.id))
            .map(s => s.subjectId)
        );

        // 4. Filtrar: semestre activo + cupos disponibles
        const delSemestre = allSections.filter(
          s => s.semesterId === activeSem.id && s.availableSeats > 0
        );

        // 5. Separar las que se pueden inscribir de las que no
        let skipped = 0;
        const disponibles = [];

        for (const s of delSemestre) {
          // Omitir si ya aprobó la asignatura
          if (approvedSubjectIds.has(s.subjectId)) {
            skipped++;
            continue;
          }
          // Omitir si ya está inscrito en esa asignatura este semestre
          if (enrolledSubjectIds.has(s.subjectId)) {
            skipped++;
            continue;
          }
          disponibles.push(s);
        }

        setSkippedCount(skipped);

        // 6. Enriquecer con nombre de asignatura, sala y horario
        const enriched = await Promise.all(
          disponibles.map(async (s) => {
            let subjectName = '—';
            let subjectCode = '—';
            let roomName    = s.roomId ? `Sala #${s.roomId}` : '—';

            try {
              const subjRes = await api.get(`/api/subjects/${s.subjectId}`);
              subjectName = subjRes.data.name ?? '—';
              subjectCode = subjRes.data.code ?? '—';
            } catch { /* mantiene default */ }

            try {
              const roomRes = await api.get(`/api/rooms/${s.roomId}`);
              roomName = roomRes.data.name ?? roomName;
            } catch { /* mantiene default */ }

            return { ...s, subjectName, subjectCode, roomName };
          })
        );

        setSections(enriched);
      } catch {
        setError('No se pudieron cargar las secciones.');
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [user.id]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!selected) { setError('Selecciona una sección.'); return; }
    setSaving(true);
    setError('');
    try {
      await api.post('/api/enrollments/enroll', {
        studentId,
        sectionId: Number(selected),
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
      setSaving(false);
    }
  };

  const selectedSection = sections.find(s => s.id === Number(selected));

  // Horario formateado para la sección seleccionada
  const scheduleLabel = (s) => {
    if (!s || s.dayOfWeek == null) return null;
    const day = DAY_NAMES[s.dayOfWeek] ?? '—';
    const start = s.startTime ? s.startTime.slice(0, 5) : '';
    const end   = s.endTime   ? s.endTime.slice(0, 5)   : '';
    return `${day} ${start}–${end}`;
  };

  return (
    <div className="container py-4" style={{ maxWidth: 560 }}>
      <div className="d-flex align-items-center gap-3 mb-4">
        <button className="btn btn-sm btn-outline-secondary"
          onClick={() => navigate('/my-enrollments')}>← Volver</button>
        <div>
          <h2 className="fw-bold mb-0">Inscribir Asignatura</h2>
          {semesterLabel && (
            <p className="text-muted mb-0 small">
              Semestre activo: <strong>{semesterLabel}</strong>
            </p>
          )}
        </div>
      </div>

      <div className="card border-0 shadow-sm">
        <div className="card-body p-4">
          {error   && <div className="alert alert-danger  py-2">{error}</div>}
          {success && <div className="alert alert-success py-2">{success}</div>}

          {loading ? (
            <div className="text-center py-4">
              <div className="spinner-border text-primary" role="status" />
              <p className="text-muted mt-2">Cargando secciones disponibles...</p>
            </div>
          ) : !error && (
            <form onSubmit={handleSubmit}>

              {/* Aviso de ramos filtrados */}
              {skippedCount > 0 && (
                <div className="alert alert-info py-2 small mb-3">
                  ℹ️ Se ocultaron <strong>{skippedCount}</strong> sección(es) de asignaturas
                  que ya aprobaste o en las que ya estás inscrito.
                </div>
              )}

              <div className="mb-3">
                <label className="form-label fw-medium">Sección disponible</label>
                {sections.length === 0 ? (
                  <div className="alert alert-warning py-2 mb-0">
                    No hay secciones disponibles para inscribir en el semestre activo.
                  </div>
                ) : (
                  <>
                    <select
                      className="form-select"
                      value={selected}
                      onChange={(e) => { setSelected(e.target.value); setError(''); }}
                    >
                      <option value="">— Selecciona una sección —</option>
                      {sections.map((s) => (
                        <option key={s.id} value={s.id}>
                          [{s.subjectCode}] {s.subjectName} · Sección #{s.id} · {s.availableSeats} cupos
                        </option>
                      ))}
                    </select>
                    <div className="form-text text-muted">
                      Solo secciones con cupos y asignaturas que puedes cursar.
                    </div>
                  </>
                )}
              </div>

              {/* Preview de la sección seleccionada */}
              {selectedSection && (
                <div className="alert alert-info py-3 mb-3 small">
                  <div className="fw-semibold mb-1">{selectedSection.subjectName}</div>
                  <div className="d-flex flex-wrap gap-3">
                    <span>👥 <strong>{selectedSection.availableSeats}</strong> / {selectedSection.totalSeats} cupos</span>
                    {scheduleLabel(selectedSection) && (
                      <span>🗓️ {scheduleLabel(selectedSection)}</span>
                    )}
                    {selectedSection.roomName && (
                      <span>🚪 {selectedSection.roomName}</span>
                    )}
                  </div>
                  <div className="text-muted mt-1">
                    ⚠️ Recuerda que si no cumples los prerrequisitos, la inscripción será rechazada.
                  </div>
                </div>
              )}

              {sections.length > 0 && (
                <button
                  type="submit"
                  className="btn btn-primary w-100 fw-semibold"
                  disabled={saving || !selected}
                >
                  {saving
                    ? <><span className="spinner-border spinner-border-sm me-2" />Inscribiendo...</>
                    : 'Confirmar inscripción'}
                </button>
              )}
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
