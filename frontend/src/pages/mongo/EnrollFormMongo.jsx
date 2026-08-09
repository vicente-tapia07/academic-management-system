import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';

const DAY_LABELS = ['', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];

function enrollmentErrorText(err) {
  const data = err.response?.data;
  const status = err.response?.status;
  const code = data?.code;

  // El $jsonSchema de enrollments exige businessRules.prerequisitesSatisfied y
  // seatAvailableAtEnrollment = true. Si la transacción intenta escribir una
  // inscripción que no los cumple, MongoDB rechaza la escritura (error 121) y
  // el backend responde 422 SCHEMA_VALIDATION_FAILED. Se muestra un mensaje
  // claro en lugar de un 500 genérico.
  if (code === 'PREREQUISITES_NOT_MET' || code === 'SCHEMA_VALIDATION_FAILED') {
    return 'MongoDB rechazó la inscripción: no cumples los prerrequisitos o el esquema académico no se satisfizo.';
  }
  if (code === 'NO_AVAILABLE_SEATS') {
    return 'No hay cupos disponibles en esta sección.';
  }
  if (code === 'ALREADY_ENROLLED') {
    return 'Ya estás inscrito en esta sección.';
  }
  if (code === 'STUDENT_NOT_ACTIVE') {
    return 'Tu estado académico no permite inscribir asignaturas.';
  }
  if (code === 'SECTION_NOT_OPEN') {
    return 'La sección no está abierta.';
  }
  if (code === 'SEMESTER_NOT_ACTIVE') {
    return 'La sección no pertenece a un semestre en curso.';
  }
  if (data?.error) {
    return String(data.error);
  }
  if (status) {
    return `No se pudo completar la inscripción (estado ${status}).`;
  }
  return 'No se pudo completar la inscripción.';
}

export default function EnrollFormMongo() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [student, setStudent] = useState(null);
  const [activeSemester, setActiveSemester] = useState(null);
  const [subjects, setSubjects] = useState([]);
  const [enrolledSectionIds, setEnrolledSectionIds] = useState([]);

  const [selectedSubjectId, setSelectedSubjectId] = useState('');
  const [sections, setSections] = useState([]);
  const [loadingSections, setLoadingSections] = useState(false);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [enrollingId, setEnrollingId] = useState(null);

  useEffect(() => {
    const init = async () => {
      try {
        const studentRes = await api.get(`/api/mongo/students/by-user/${user.id}`);
        const me = studentRes.data;
        setStudent(me);

        const [semesterRes, subjectsRes, enrollmentsRes] = await Promise.all([
          api.get('/api/mongo/semesters/active'),
          api.get('/api/mongo/subjects'),
          api.get(`/api/mongo/enrollments/student/${me.id}`),
        ]);
        setActiveSemester(semesterRes.data);
        setSubjects(subjectsRes.data);

        const activeEnrolled = new Set(
          enrollmentsRes.data
            .filter((e) => e.status === 'ACTIVE' || e.status === 'COMPLETED')
            .map((e) => e.sectionId)
        );
        setEnrolledSectionIds(activeEnrolled);

        if (subjectsRes.data.length > 0) {
          setSelectedSubjectId(subjectsRes.data[0].id);
        }
      } catch (err) {
        const data = err.response?.data;
        setError(
          data?.error || 'No se pudieron cargar los datos de inscripción (MongoDB).'
        );
      } finally {
        setLoading(false);
      }
    };
    init();
  }, [user.id]);

  const fetchSections = useCallback(async (subjectId) => {
    if (!subjectId || !activeSemester) return;
    setLoadingSections(true);
    setError('');
    try {
      const res = await api.get('/api/mongo/sections', {
        params: { subjectId, semesterId: activeSemester.id },
      });
      setSections(res.data);
    } catch {
      setSections([]);
      setError('Error al cargar las secciones disponibles.');
    } finally {
      setLoadingSections(false);
    }
  }, [activeSemester]);

  useEffect(() => {
    fetchSections(selectedSubjectId);
  }, [selectedSubjectId, fetchSections]);

  const handleSubjectChange = (e) => {
    setSelectedSubjectId(e.target.value);
    setError('');
    setSuccess('');
  };

  const handleEnroll = async (sectionId) => {
    if (!student || !sectionId) return;
    setEnrollingId(sectionId);
    setError('');
    setSuccess('');
    try {
      await api.post('/api/mongo/enrollments/enroll', {
        studentId: student.id,
        sectionId,
      });
      setSuccess('¡Inscripción exitosa en MongoDB!');
      setTimeout(() => navigate('/mongo/my-enrollments'), 1500);
    } catch (err) {
      setError(enrollmentErrorText(err));
    } finally {
      setEnrollingId(null);
    }
  };

  const visibleSections = sections.filter(
    (s) => !enrolledSectionIds.has(s.id)
  );

  const selectedSubject = subjects.find((s) => s.id === selectedSubjectId);

  const dayLabel = (day) => DAY_LABELS[day] || `Día ${day}`;

  return (
    <div className="container py-4" style={{ maxWidth: 820 }}>
      <div className="d-flex align-items-center gap-3 mb-4">
        <button
          className="btn btn-sm btn-outline-secondary"
          onClick={() => navigate('/mongo/my-enrollments')}
        >
          ← Volver
        </button>
        <div>
          <h2 className="fw-bold mb-0">Inscribir Asignatura (MongoDB)</h2>
          <p className="text-muted mb-0 small">
            Flujo del Laboratorio 3 · Transacciones + Schema Validation
          </p>
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
            {error && <div className="alert alert-danger py-2">{error}</div>}
            {success && <div className="alert alert-success py-2">{success}</div>}

            {!student ? (
              <div className="alert alert-warning py-2 mb-0">
                No hay un estudiante MongoDB asociado a tu usuario. Revisa que el
                seed de Mongo esté cargado y tu `userId` coincida con el del
                estudiante.
              </div>
            ) : (
              <>
                <div className="alert alert-light border py-2 small mb-3">
                  👤 <strong>{student.firstName} {student.lastName}</strong> ·{' '}
                  Matrícula {student.enrollmentNumber} ·{' '}
                  Estado{' '}
                  <span
                    className={`badge ${
                      student.academicStatus === 'ACTIVE'
                        ? 'bg-success'
                        : 'bg-secondary'
                    }`}
                  >
                    {student.academicStatus}
                  </span>
                  {activeSemester && (
                    <span className="ms-2">
                      · Semestre activo:{' '}
                      <strong>{activeSemester.year} — {activeSemester.period}</strong>
                    </span>
                  )}
                </div>

                <div className="mb-3">
                  <label className="form-label fw-semibold">Asignatura</label>
                  {subjects.length === 0 ? (
                    <div className="alert alert-warning py-2 mb-0">
                      No hay asignaturas activas en el catálogo MongoDB.
                    </div>
                  ) : (
                    <select
                      className="form-select"
                      value={selectedSubjectId}
                      onChange={handleSubjectChange}
                    >
                      {subjects.map((sub) => (
                        <option key={sub.id} value={sub.id}>
                          {sub.code} — {sub.name}
                        </option>
                      ))}
                    </select>
                  )}
                </div>

                {selectedSubject && (
                  <>
                    <p className="text-muted small mb-2">
                      <strong>{selectedSubject.code}</strong> —{' '}
                      {selectedSubject.name} · {selectedSubject.credits} créditos
                    </p>

                    {loadingSections && (
                      <div className="text-center py-3">
                        <div className="spinner-border spinner-border-sm text-primary" role="status" />
                      </div>
                    )}

                    {!loadingSections && visibleSections.length === 0 && (
                      <div className="alert alert-info py-2 small">
                        {sections.length === 0
                          ? 'No hay secciones abiertas con cupo para esta asignatura en el semestre activo.'
                          : 'Ya estás inscrito en todas las secciones disponibles de esta asignatura.'}
                      </div>
                    )}

                    {!loadingSections && visibleSections.length > 0 && (
                      <div className="list-group mb-3">
                        {visibleSections.map((s) => (
                          <div
                            key={s.id}
                            className="list-group-item d-flex justify-content-between align-items-center flex-wrap gap-2"
                          >
                            <div>
                              <div className="fw-semibold small">
                                {selectedSubject.code} — {selectedSubject.name}
                              </div>
                              <div className="text-muted small">
                                👨‍🏫 {s.professorName} · 🚪 {s.room?.code} ({s.room?.building})
                              </div>
                              <div className="text-muted small">
                                🗓️ {dayLabel(s.schedule?.dayOfWeek)} {s.schedule?.startTime}–{s.schedule?.endTime}
                              </div>
                              <div className="mt-1">
                                <span className={`badge ${s.availableSeats > 0 ? 'bg-success' : 'bg-danger'}`}>
                                  Cupos: {s.availableSeats} / {s.totalSeats}
                                </span>
                              </div>
                            </div>
                            <button
                              className="btn btn-primary btn-sm"
                              onClick={() => handleEnroll(s.id)}
                              disabled={enrollingId === s.id}
                            >
                              {enrollingId === s.id ? 'Inscribiendo...' : 'Inscribir'}
                            </button>
                          </div>
                        ))}
                      </div>
                    )}
                  </>
                )}
              </>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
