import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import api from '../../services/api';

const DAY_NAMES = ['Dom', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb'];

export default function StudentGrades() {
  const { sectionId } = useParams();
  const navigate      = useNavigate();

  const [enrollments, setEnrollments] = useState([]);
  const [grades,      setGrades]      = useState([]);
  const [students,    setStudents]    = useState([]);
  const [section,     setSection]     = useState(null);
  const [subject,     setSubject]     = useState(null);
  const [room,        setRoom]        = useState(null);
  const [loading,     setLoading]     = useState(true);
  const [error,       setError]       = useState('');

  useEffect(() => {
    const load = async () => {
      try {
        const [enrollRes, gradeRes, studentRes, sectionRes] = await Promise.all([
          api.get(`/api/enrollments/section/${sectionId}`),
          api.get('/api/grades'),
          api.get('/api/students'),
          api.get(`/api/sections/${sectionId}`),
        ]);
        setEnrollments(enrollRes.data);
        setGrades(gradeRes.data);
        setStudents(studentRes.data);
        const sec = sectionRes.data;
        setSection(sec);

        // Enriquecer con asignatura y sala
        try {
          const subRes = await api.get(`/api/subjects/${sec.subjectId}`);
          setSubject(subRes.data);
        } catch { /* sin asignatura */ }

        try {
          const roomRes = await api.get(`/api/rooms/${sec.roomId}`);
          setRoom(roomRes.data);
        } catch { /* sin sala */ }

      } catch {
        setError('Error al cargar los datos de la sección.');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [sectionId]);

  const gradeFor = (enrollmentId) =>
    grades.find((g) => g.enrollmentId === enrollmentId);

  const getStudent = (studentId) =>
    students.find((s) => s.id === studentId);

  const studentName = (studentId) => {
    const s = getStudent(studentId);
    return s ? `${s.firstName} ${s.lastName}` : `Estudiante #${studentId}`;
  };

  const studentRut = (studentId) => {
    // El RUT está en usuario, no en student.
    // Por ahora mostramos el enrollmentNumber como identificador
    const s = getStudent(studentId);
    return s?.enrollmentNumber ?? '—';
  };

  if (loading) return <p className="text-muted p-4">Cargando...</p>;
  if (error)   return <div className="alert alert-danger m-4">{error}</div>;

  const scheduleLabel = section
    ? `${section.dayOfWeek != null ? DAY_NAMES[section.dayOfWeek] : '—'} ${section.startTime?.slice(0,5) ?? ''}–${section.endTime?.slice(0,5) ?? ''}`
    : '—';

  return (
    <div className="container py-4">
      <div className="d-flex align-items-center gap-3 mb-4">
        <button className="btn btn-outline-secondary"
          onClick={() => navigate('/professor/courses')}>
          ← Volver
        </button>
        <div>
          <h2 className="fw-bold mb-0">
            {subject ? `${subject.code} — ${subject.name}` : `Sección #${sectionId}`}
          </h2>
          <p className="text-muted mb-0 small">
            Sección #{sectionId}
            {room && <> · 🚪 {room.name}</>}
            {section?.dayOfWeek != null && <> · 🗓️ {scheduleLabel}</>}
            {' · '}
            👥 {section ? section.totalSeats - section.availableSeats : '—'} / {section?.totalSeats} inscritos
          </p>
        </div>
      </div>

      <div className="card shadow-sm border-0">
        <div className="table-responsive">
          <table className="table table-hover mb-0 align-middle">
            <thead className="table-light">
              <tr>
                <th>Matrícula</th>
                <th>Estudiante</th>
                <th>Estado</th>
                <th>Nota</th>
                <th className="text-end">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {enrollments.length === 0 && (
                <tr>
                  <td colSpan={5} className="text-center text-muted py-4">
                    No hay estudiantes inscritos en esta sección.
                  </td>
                </tr>
              )}
              {enrollments.map((enroll) => {
                const grade = gradeFor(enroll.id);
                return (
                  <tr key={enroll.id}>
                    <td>
                      <span className="badge bg-secondary font-monospace">
                        {studentRut(enroll.studentId)}
                      </span>
                    </td>
                    <td className="fw-semibold">{studentName(enroll.studentId)}</td>
                    <td>
                      <span className={`badge ${
                        enroll.status === 'ACTIVE'    ? 'bg-success' :
                        enroll.status === 'COMPLETED' ? 'bg-primary' : 'bg-secondary'
                      }`}>
                        {enroll.status}
                      </span>
                    </td>
                    <td>
                      {grade ? (
                        <span className={`fw-bold ${grade.value >= 4.0 ? 'text-success' : 'text-danger'}`}>
                          {grade.value.toFixed(1)}
                        </span>
                      ) : (
                        <span className="text-muted fst-italic">Sin nota</span>
                      )}
                    </td>
                    <td className="text-end">
                      <button
                        className="btn btn-sm btn-outline-primary"
                        onClick={() => navigate('/professor/grade/new', {
                          state: {
                            enrollmentId:  enroll.id,
                            studentName:   studentName(enroll.studentId),
                            existingGrade: grade ?? null,
                          }
                        })}>
                        {grade ? 'Editar nota' : 'Ingresar nota'}
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
