import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../services/api';

export default function StudentEnrollAdmin() {
  const navigate = useNavigate();

  const [students, setStudents] = useState([]);
  const [sections, setSections] = useState([]);
  const [subjects, setSubjects] = useState([]);

  const [selectedStudent, setSelectedStudent] = useState('');
  const [selectedSection, setSelectedSection] = useState('');

  const [loading, setLoading] = useState(true);
  const [saving,  setSaving]  = useState(false);
  const [error,   setError]   = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    const load = async () => {
      try {
        const [studRes, secRes, subRes] = await Promise.all([
          api.get('/api/students'),
          api.get('/api/sections'),
          api.get('/api/subjects'),
        ]);
        setStudents(studRes.data);
        setSections(secRes.data.filter(s => s.availableSeats > 0));
        setSubjects(subRes.data);
      } catch {
        setError('Error al cargar los datos.');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const subjectName = (id) => subjects.find(s => s.id === id)?.name ?? `#${id}`;
  const subjectCode = (id) => subjects.find(s => s.id === id)?.code ?? '---';

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!selectedStudent || !selectedSection) {
      setError('Debes seleccionar un estudiante y una sección.');
      return;
    }
    setSaving(true);
    setError('');
    setSuccess('');
    try {
      await api.post('/api/enrollments/enroll', {
        studentId: Number(selectedStudent),
        sectionId: Number(selectedSection),
      });
      setSuccess('¡Estudiante inscrito correctamente!');
      setSelectedStudent('');
      setSelectedSection('');
    } catch (err) {
        const msg = (err.response?.data ?? '').toString();
        if (msg.toLowerCase().includes('prerequisit')) {
            setError('El estudiante no cumple los prerrequisitos.');
        } else if (msg.toLowerCase().includes('cupo')) {
            setError('No hay cupos disponibles.');
        } else if (msg.toLowerCase().includes('unicidad') || msg.toLowerCase().includes('unique') || msg.toLowerCase().includes('enrollment_student')) {
            setError('Este estudiante ya está inscrito en esa sección.');
        } else {
            setError(msg || 'Error al inscribir al estudiante.');
        }
      } finally {
      setSaving(false);
    }
  };

  const seccionSeleccionada = sections.find(s => s.id === Number(selectedSection));

  if (loading) return <p className="text-muted p-4">Cargando...</p>;

  return (
    <div className="container py-4" style={{ maxWidth: 560 }}>
      <div className="d-flex align-items-center gap-3 mb-4">
        <button className="btn btn-outline-secondary" onClick={() => navigate('/students')}>
          ← Volver
        </button>
        <div>
          <h2 className="fw-bold mb-0">Inscribir Estudiante</h2>
          <p className="text-muted mb-0">El admin inscribe a un estudiante en una sección</p>
        </div>
      </div>

      {error   && <div className="alert alert-danger">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      <div className="card shadow-sm border-0">
        <div className="card-body p-4">
          <form onSubmit={handleSubmit}>

            {/* Selector de estudiante */}
            <div className="mb-3">
              <label className="form-label fw-semibold">Estudiante</label>
              <select className="form-select" value={selectedStudent}
                onChange={(e) => { setSelectedStudent(e.target.value); setError(''); }}>
                <option value="">— Selecciona un estudiante —</option>
                {students.map(s => (
                  <option key={s.id} value={s.id}>
                    {s.enrollmentNumber} · {s.firstName} {s.lastName}
                  </option>
                ))}
              </select>
            </div>

            {/* Selector de sección */}
            <div className="mb-3">
              <label className="form-label fw-semibold">Sección</label>
              <select className="form-select" value={selectedSection}
                onChange={(e) => { setSelectedSection(e.target.value); setError(''); }}>
                <option value="">— Selecciona una sección —</option>
                {sections.map(s => (
                  <option key={s.id} value={s.id}>
                    {subjectCode(s.subjectId)} · {subjectName(s.subjectId)} — Sección #{s.id} ({s.availableSeats} cupos)
                  </option>
                ))}
              </select>
            </div>

            {/* Detalle de la sección seleccionada */}
            {seccionSeleccionada && (
              <div className="alert alert-info py-2 small mb-3">
                <strong>{subjectName(seccionSeleccionada.subjectId)}</strong>
                {' · '}Cupos disponibles: {seccionSeleccionada.availableSeats}/{seccionSeleccionada.totalSeats}
              </div>
            )}

            <div className="d-flex gap-2 mt-2">
              <button type="submit" className="btn btn-primary flex-grow-1" disabled={saving}>
                {saving ? 'Inscribiendo...' : 'Confirmar inscripción'}
              </button>
              <button type="button" className="btn btn-outline-secondary"
                onClick={() => navigate('/students')}>
                Cancelar
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}