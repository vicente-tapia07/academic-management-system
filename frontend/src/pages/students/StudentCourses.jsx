import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';

const DAY_NAMES = ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];

export default function StudentCourses() {
  const { user }   = useAuth();
  const navigate   = useNavigate();
  const [courses,  setCourses]  = useState([]);
  const [loading,  setLoading]  = useState(true);
  const [error,    setError]    = useState('');

  useEffect(() => {
    const load = async () => {
      try {
        // Traer secciones activas del estudiante
        const secRes = await api.get(`/api/sections/student/${user.id}`);
        const sections = secRes.data;

        // Enriquecer cada sección con nombre de asignatura y sala
        const enriched = await Promise.all(sections.map(async (s) => {
          let subjectName = `Asignatura #${s.subjectId}`;
          let subjectCode = '---';
          let roomName    = `Sala #${s.roomId}`;
          let buildingName = '';

          try {
            const subRes = await api.get(`/api/subjects/${s.subjectId}`);
            subjectName = subRes.data.name;
            subjectCode = subRes.data.code;
          } catch { /* mantiene default */ }

          try {
            const roomRes = await api.get(`/api/rooms/${s.roomId}`);
            roomName = roomRes.data.name;
            const buildRes = await api.get(`/api/buildings/${roomRes.data.buildingId}`);
            buildingName = buildRes.data.name;
          } catch { /* mantiene default */ }

          return { ...s, subjectName, subjectCode, roomName, buildingName };
        }));

        setCourses(enriched);
      } catch {
        setError('No se pudieron cargar los cursos.');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [user.id]);

  return (
    <div className="container py-4">
      <div className="d-flex align-items-center gap-3 mb-4">
        <button className="btn btn-sm btn-outline-secondary" onClick={() => navigate('/my-dashboard')}>
          ← Volver
        </button>
        <div>
          <h2 className="fw-bold mb-0">Mis Cursos</h2>
          <p className="text-muted mb-0 small">Secciones activas del semestre en curso</p>
        </div>
      </div>

      {loading && (
        <div className="text-center py-5">
          <div className="spinner-border text-primary" role="status" />
          <p className="text-muted mt-2">Cargando cursos...</p>
        </div>
      )}
      {error && <div className="alert alert-danger">{error}</div>}

      {!loading && !error && courses.length === 0 && (
        <div className="alert alert-info">No tienes cursos activos en el semestre actual.</div>
      )}

      {!loading && !error && courses.length > 0 && (
        <div className="row g-3">
          {courses.map((c) => (
            <div key={c.id} className="col-md-6">
              <div className="card border-0 shadow-sm h-100">
                <div className="card-body p-4">

                  {/* Encabezado de la tarjeta */}
                  <div className="d-flex justify-content-between align-items-start mb-2">
                    <span className="badge bg-primary font-monospace fs-6">{c.subjectCode}</span>
                    <span className="badge bg-secondary">Sección #{c.id}</span>
                  </div>

                  <h5 className="fw-bold mb-3">{c.subjectName}</h5>

                  {/* Horario */}
                  <div className="d-flex align-items-center gap-2 mb-2">
                    <span className="text-primary">🗓️</span>
                    <span className="small">
                      <strong>{c.dayOfWeek != null ? DAY_NAMES[c.dayOfWeek] : '—'}</strong>
                      {c.startTime && c.endTime
                        ? ` · ${c.startTime} – ${c.endTime}`
                        : ''}
                    </span>
                  </div>

                  {/* Sala */}
                  <div className="d-flex align-items-center gap-2 mb-2">
                    <span className="text-primary">🚪</span>
                    <span className="small">
                      <strong>{c.roomName}</strong>
                      {c.buildingName ? ` · ${c.buildingName}` : ''}
                    </span>
                  </div>

                  {/* Cupos */}
                  <div className="d-flex align-items-center gap-2">
                    <span className="text-primary">👥</span>
                    <span className="small text-muted">
                      {c.availableSeats} cupos disponibles de {c.totalSeats}
                    </span>
                  </div>

                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
