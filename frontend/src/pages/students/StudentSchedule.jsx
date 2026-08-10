import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';

// Bloques horarios oficiales USACH
const BLOCKS = [
  { label: '1°',  start: '08:15', end: '09:35' },
  { label: '2°',  start: '09:50', end: '11:10' },
  { label: '3°',  start: '11:25', end: '12:45' },
  { label: '4°',  start: '13:45', end: '15:05' },
  { label: '5°',  start: '15:20', end: '16:40' },
  { label: '6°',  start: '16:55', end: '18:15' },
  { label: '7°',  start: '18:45', end: '20:05' },
  { label: '8°',  start: '20:05', end: '21:25' },
  { label: '9°',  start: '21:25', end: '22:45' },
];

const WEEK_DAYS = [
  { index: 1, name: 'Lunes'     },
  { index: 2, name: 'Martes'    },
  { index: 3, name: 'Miércoles' },
  { index: 4, name: 'Jueves'    },
  { index: 5, name: 'Viernes'   },
  { index: 6, name: 'Sábado'    },
];

const COLORS = [
  '#003366', '#0077cc', '#198754',
  '#dc3545', '#fd7e14', '#6610f2',
];

// Normaliza "HH:MM:SS" o "HH:MM" a "HH:MM"
function normalizeTime(t) {
  if (!t) return '';
  return t.slice(0, 5);
}

// Determina en qué bloque cae una sección comparando HH:MM
function findBlock(startTime, endTime) {
  const s = normalizeTime(startTime);
  const e = normalizeTime(endTime);
  return BLOCKS.findIndex((b) => b.start === s && b.end === e);
}

export default function StudentSchedule() {
  const { user }  = useAuth();
  const navigate  = useNavigate();
  const [courses, setCourses]  = useState([]);
  const [loading, setLoading]  = useState(true);
  const [error,   setError]    = useState('');

  useEffect(() => {
    const load = async () => {
      try {
        const secRes = await api.get(`/api/sections/student/${user.id}`);
        const sections = secRes.data;

        const enriched = await Promise.all(sections.map(async (s, index) => {
          let subjectName = `Asignatura #${s.subjectId}`;
          let subjectCode = '---';

          try {
            const subRes = await api.get(`/api/subjects/${s.subjectId}`);
            subjectName = subRes.data.name;
            subjectCode = subRes.data.code;
          } catch { /* mantiene default */ }

          return {
            ...s,
            subjectName,
            subjectCode,
            roomName: s.room?.name ?? '—',
            color: COLORS[index % COLORS.length],
          };
        }));

        setCourses(enriched);
      } catch {
        setError('No se pudo cargar el horario.');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [user.id]);

  const todayIndex = new Date().getDay(); // 0=dom, 1=lun...

  // Cursos que caen en un día y bloque específico
  const getCourseForCell = (dayIndex, blockIndex) =>
    courses.find((c) => {
      if (c.dayOfWeek !== dayIndex) return false;
      return findBlock(c.startTime, c.endTime) === blockIndex;
    }) ?? null;

  return (
    <div className="container-fluid py-4" style={{ maxWidth: 1100 }}>
      <div className="d-flex align-items-center gap-3 mb-4">
        <button className="btn btn-sm btn-outline-secondary" onClick={() => navigate('/my-dashboard')}>
          ← Volver
        </button>
        <div>
          <h2 className="fw-bold mb-0">Mi Horario</h2>
          <p className="text-muted mb-0 small">Semestre en curso · Bloques oficiales USACH</p>
        </div>
      </div>

      {loading && (
        <div className="text-center py-5">
          <div className="spinner-border text-primary" role="status" />
          <p className="text-muted mt-2">Cargando horario...</p>
        </div>
      )}
      {error && <div className="alert alert-danger">{error}</div>}

      {!loading && !error && courses.length === 0 && (
        <div className="alert alert-info">No tienes cursos activos en el semestre actual.</div>
      )}

      {!loading && !error && courses.length > 0 && (
        <>
          {/* Leyenda */}
          <div className="d-flex flex-wrap gap-2 mb-3">
            {courses.map((c) => (
              <span key={c.id} className="badge px-2 py-2"
                style={{ backgroundColor: c.color, color: '#fff', fontSize: '0.8rem' }}>
                <strong>{c.subjectCode}</strong> · {c.subjectName}
              </span>
            ))}
          </div>

          {/* Grilla */}
          <div className="card border-0 shadow-sm overflow-auto">
            <table className="table table-bordered mb-0" style={{ tableLayout: 'fixed', minWidth: 700 }}>
              <thead>
                <tr style={{ backgroundColor: '#003366', color: '#fff' }}>
                  {/* Columna de bloques */}
                  <th style={{ width: 110, textAlign: 'center', verticalAlign: 'middle' }}>
                    Bloque
                  </th>
                  {WEEK_DAYS.map((d) => {
                    const isToday = d.index === todayIndex;
                    return (
                      <th key={d.index} style={{
                        textAlign:       'center',
                        verticalAlign:   'middle',
                        backgroundColor: isToday ? '#0056b3' : '#003366',
                      }}>
                        {d.name}
                        {isToday && (
                          <span className="badge bg-warning text-dark ms-1" style={{ fontSize: '0.65rem' }}>
                            Hoy
                          </span>
                        )}
                      </th>
                    );
                  })}
                </tr>
              </thead>
              <tbody>
                {BLOCKS.map((block, blockIdx) => (
                  <tr key={blockIdx}>
                    {/* Celda de bloque/horario */}
                    <td style={{
                      textAlign:    'center',
                      verticalAlign:'middle',
                      backgroundColor: '#f8f9fa',
                      fontSize:     '0.78rem',
                      lineHeight:   1.3,
                      padding:      '6px 4px',
                    }}>
                      <div className="fw-bold">{block.label}</div>
                      <div className="text-muted" style={{ fontSize: '0.72rem' }}>
                        {block.start}
                      </div>
                      <div className="text-muted" style={{ fontSize: '0.72rem' }}>
                        {block.end}
                      </div>
                    </td>

                    {/* Celdas de días */}
                    {WEEK_DAYS.map((day) => {
                      const course = getCourseForCell(day.index, blockIdx);
                      const isToday = day.index === todayIndex;
                      return (
                        <td key={day.index} style={{
                          padding:         4,
                          verticalAlign:   'middle',
                          backgroundColor: isToday ? '#f0f4ff' : '#fff',
                          height:          64,
                        }}>
                          {course ? (
                            <div style={{
                              backgroundColor: course.color,
                              color:           '#fff',
                              borderRadius:    6,
                              padding:         '5px 8px',
                              fontSize:        '0.78rem',
                              lineHeight:      1.3,
                              height:          '100%',
                              display:         'flex',
                              flexDirection:   'column',
                              justifyContent:  'center',
                            }}>
                              <div className="fw-bold">{course.subjectCode}</div>
                              <div style={{ fontSize: '0.7rem', opacity: 0.9 }}>
                                {course.subjectName}
                              </div>
                              <div style={{ fontSize: '0.68rem', opacity: 0.8 }}>
                                🚪 {course.roomName}
                              </div>
                            </div>
                          ) : (
                            <div style={{
                              height:          '100%',
                              display:         'flex',
                              alignItems:      'center',
                              justifyContent:  'center',
                              color:           '#dee2e6',
                              fontSize:        '1rem',
                            }}>
                              —
                            </div>
                          )}
                        </td>
                      );
                    })}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Detalle en tabla */}
          <h6 className="fw-semibold mt-4 mb-2">Detalle de clases</h6>
          <div className="card border-0 shadow-sm">
            <div className="table-responsive">
              <table className="table table-sm table-hover mb-0">
                <thead className="table-light">
                  <tr>
                    <th>Asignatura</th>
                    <th>Día</th>
                    <th>Bloque</th>
                    <th>Horario</th>
                    <th>Sala</th>
                  </tr>
                </thead>
                <tbody>
                  {[...courses]
                    .sort((a, b) => (a.dayOfWeek ?? 0) - (b.dayOfWeek ?? 0))
                    .map((c) => {
                      const blockIdx = findBlock(c.startTime, c.endTime);
                      const block = blockIdx >= 0 ? BLOCKS[blockIdx] : null;
                      const dayName = WEEK_DAYS.find(d => d.index === c.dayOfWeek)?.name ?? '—';
                      return (
                        <tr key={c.id}>
                          <td>
                            <span className="badge me-1" style={{ backgroundColor: c.color }}>
                              {c.subjectCode}
                            </span>
                            <span className="fw-semibold">{c.subjectName}</span>
                          </td>
                          <td>{dayName}</td>
                          <td className="text-center">{block ? block.label : '—'}</td>
                          <td className="text-nowrap text-muted small">
                            {block ? `${block.start} – ${block.end}` : '—'}
                          </td>
                          <td className="text-muted small">{c.roomName}</td>
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
