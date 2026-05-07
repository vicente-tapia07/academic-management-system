import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';

export default function EnrollForm() {
  const { user }              = useAuth();
  const navigate              = useNavigate();
  const [sections,  setSections]  = useState([]);
  const [selected,  setSelected]  = useState('');
  const [loading,   setLoading]   = useState(true);
  const [saving,    setSaving]    = useState(false);
  const [error,     setError]     = useState('');
  const [success,   setSuccess]   = useState('');
  const [studentId, setStudentId] = useState(null);
  const [semesterLabel, setSemesterLabel] = useState('');

  useEffect(() => {
    const fetchData = async () => {
      try {
        // 1. Obtener el ID del estudiante
        const studRes = await api.get('/api/students');
        const me = studRes.data.find(s => s.usuarioId === user.id);
        if (!me) throw new Error('Estudiante no encontrado');
        setStudentId(me.id);

        // 2. Buscar el semestre activo (IN_PROGRESS)
        const semRes = await api.get('/api/semesters');
        const activeSem = semRes.data.find(s => s.status === 'IN_PROGRESS');

        if (!activeSem) {
          setError('No hay un semestre activo en este momento. No es posible inscribirse.');
          setLoading(false);
          return;
        }

        setSemesterLabel(`${activeSem.year} — ${activeSem.period}`);

        // 3. Traer todas las secciones y filtrar por semestre activo + cupos
        const secRes = await api.get('/api/sections');
        const delSemestre = secRes.data.filter(
          s => s.semesterId === activeSem.id && s.availableSeats > 0
        );

        // 4. Enriquecer con nombre de asignatura
        const enriched = await Promise.all(
          delSemestre.map(async (s) => {
            try {
              const subjRes = await api.get(`/api/subjects/${s.subjectId}`);
              return {
                ...s,
                subjectName: subjRes.data.name ?? '—',
                subjectCode: subjRes.data.code ?? '—',
              };
            } catch {
              return { ...s, subjectName: '—', subjectCode: '—' };
            }
          })
        );

        setSections(enriched);
      } catch (err) {
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
        studentId: studentId,
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

  return (
    <div className="container py-4" style={{ maxWidth: 540 }}>
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
              <p className="text-muted mt-2">Cargando secciones...</p>
            </div>
          ) : !error && (
            <form onSubmit={handleSubmit}>
              <div className="mb-3">
                <label className="form-label fw-medium">Sección disponible</label>
                {sections.length === 0 ? (
                  <div className="alert alert-warning py-2 mb-0">
                    No hay secciones disponibles en el semestre activo.
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
                          [{s.subjectCode}] {s.subjectName} — Sección #{s.id} ({s.availableSeats} cupos)
                        </option>
                      ))}
                    </select>
                    <div className="form-text text-muted">
                      Solo secciones del semestre activo con cupos disponibles.
                    </div>
                  </>
                )}
              </div>

              {selectedSection && (
                <div className="alert alert-info py-2 mb-3 small">
                  <strong>{selectedSection.subjectName}</strong>
                  {' · '}Cupos disponibles: <strong>{selectedSection.availableSeats}</strong> / {selectedSection.totalSeats}
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