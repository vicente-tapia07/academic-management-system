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

  useEffect(() => {
    const fetchSections = async () => {
      try {
        const res = await api.get('/api/sections');
        const raw = res.data;

        // Por cada sección busca el nombre de la asignatura
        const enriched = await Promise.all(
          raw.map(async (s) => {
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

        // Solo muestra secciones con cupos disponibles
        setSections(enriched.filter(s => s.availableSeats > 0));
      } catch {
        setError('No se pudieron cargar las secciones.');
      } finally {
        setLoading(false);
      }
    };
    fetchSections();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!selected) { setError('Selecciona una sección.'); return; }
    setSaving(true);
    setError('');
    try {
      await api.post('/api/enrollments/enroll', {
        studentId: user.id,
        sectionId: Number(selected),
      });
      setSuccess('¡Inscripción exitosa!');
      setTimeout(() => navigate('/my-enrollments'), 1500);
    } catch (err) {
      const msg = err.response?.data?.message ?? err.response?.data ?? '';
      const msgStr = msg.toString().toLowerCase();
      if (msgStr.includes('prerequisit') || msgStr.includes('requisito')) {
        setError('No cumples los prerrequisitos para esta asignatura.');
      } else if (msgStr.includes('cupo') || msgStr.includes('slot') || msgStr.includes('seat')) {
        setError('No hay cupos disponibles en esta sección.');
      } else if (err.response?.status === 409) {
        setError('Ya estás inscrito en esta asignatura.');
      } else {
        setError('No se pudo completar la inscripción.');
      }
    } finally {
      setSaving(false);
    }
  };

  // Sección seleccionada para mostrar detalle
  const selectedSection = sections.find(s => s.id === Number(selected));

  return (
    <div className="container py-4" style={{ maxWidth: 540 }}>
      <div className="d-flex align-items-center gap-3 mb-4">
        <button className="btn btn-sm btn-outline-secondary"
          onClick={() => navigate('/my-enrollments')}>
          ← Volver
        </button>
        <h2 className="fw-bold mb-0">Inscribir Asignatura</h2>
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
          ) : (
            <form onSubmit={handleSubmit}>
              <div className="mb-3">
                <label className="form-label fw-medium">Sección disponible</label>
                <select
                  className="form-select"
                  value={selected}
                  onChange={(e) => { setSelected(e.target.value); setError(''); }}
                >
                  <option value="">— Selecciona una sección —</option>
                  {sections.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.subjectCode} · {s.subjectName} — Sección #{s.id} ({s.availableSeats} cupos)
                    </option>
                  ))}
                </select>
                <div className="form-text text-muted">
                  Solo se muestran secciones con cupos disponibles.
                </div>
              </div>

              {/* Detalle de la sección seleccionada */}
              {selectedSection && (
                <div className="alert alert-info py-2 mb-3 small">
                  <strong>{selectedSection.subjectName}</strong>
                  {' · '}Cupos: {selectedSection.availableSeats} / {selectedSection.totalSeats}
                </div>
              )}

              <button
                type="submit"
                className="btn w-100 text-white fw-semibold"
                style={{ backgroundColor: '#003366' }}
                disabled={saving || !selected}
              >
                {saving ? (
                  <><span className="spinner-border spinner-border-sm me-2" />Inscribiendo...</>
                ) : 'Confirmar inscripción'}
              </button>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}