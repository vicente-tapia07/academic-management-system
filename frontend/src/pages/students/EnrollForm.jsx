import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import useGeolocation from '../../hooks/useGeolocation';
import MapView from '../../components/MapView';
import api from '../../services/api';

const NEARBY_THRESHOLD_M = 150; // umbral "muy cercana" pedido en el enunciado (I3)

export default function EnrollForm() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const { position, error: geoError, loading: geoLoading, requestLocation } = useGeolocation();

  // ── Estado académico (lo que ya existía en EnrollForm) ──
  const [studentId,     setStudentId]     = useState(null);
  const [activeSemester,setActiveSemester]= useState(null);
  const [subjects,      setSubjects]      = useState([]);       // asignaturas disponibles para inscribir
  const [selectedSubjectId, setSelectedSubjectId] = useState('');
  const [skippedCount,  setSkippedCount]  = useState(0);
  const [loading,       setLoading]       = useState(true);
  const [error,         setError]         = useState('');
  const [success,       setSuccess]       = useState('');

  // ── Estado geoespacial (lo que existía en DistanceReport — Integrante 3) ──
  const [sections,         setSections]         = useState([]); // secciones con distancia, de la asignatura elegida
  const [showOnlyNearby,   setShowOnlyNearby]    = useState(false);
  const [loadingSections,  setLoadingSections]   = useState(false);
  const [enrollingId,      setEnrollingId]       = useState(null);

  const formatDistance = (meters) => {
    if (meters == null) return { text: '—', isFar: false };
    if (meters >= 1000) return { text: `${(meters / 1000).toFixed(1)} km`, isFar: true };
    return { text: `${Math.round(meters)} m`, isFar: meters > 300 };
  };

  // ── 1. Carga inicial: estudiante, semestre activo, malla, inscripciones ──
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
        if (disponibles.length > 0) setSelectedSubjectId(String(disponibles[0].id));
      } catch {
        setError('No se pudieron cargar los datos de inscripción.');
      } finally {
        setLoading(false);
      }
    };
    init();
  }, [user.id]);

  // ── 2. Pedir ubicación al montar (Integrante 3) ──
  useEffect(() => { requestLocation(); }, [requestLocation]);

  // ── 3. Buscar secciones cercanas cuando cambia asignatura o ubicación ──
  const fetchNearbySections = useCallback(async (subjectId, lat, lng) => {
    setLoadingSections(true);
    setError('');
    try {
      const res = await api.get('/api/enrollments/nearby-sections', {
        params: { subjectId, lat, lng },
      });
      // El backend ya filtra por semestre activo y cupos (ver enunciado I3),
      // pero validamos igual por si acaso.
      setSections(res.data);
    } catch {
      setError('Error al cargar las secciones cercanas.');
      setSections([]);
    } finally {
      setLoadingSections(false);
    }
  }, []);

  useEffect(() => {
    if (selectedSubjectId && position) {
      fetchNearbySections(selectedSubjectId, position.lat, position.lng);
    } else {
      setSections([]);
    }
  }, [selectedSubjectId, position, fetchNearbySections]);

  const visibleSections = showOnlyNearby
    ? sections.filter((s) => s.distanceMeters <= NEARBY_THRESHOLD_M)
    : sections;

  const handleSubjectChange = (e) => {
    setSelectedSubjectId(e.target.value);
    setError(''); setSuccess('');
  };

  // ── 4. Inscribir (con el manejo robusto de errores de EnrollForm) ──
  const handleEnroll = async (sectionId) => {
    if (!studentId || !sectionId) return;
    setEnrollingId(sectionId);
    setError(''); setSuccess('');
    try {
      await api.post('/api/enrollments/enroll', {
        studentId,
        sectionId: Number(sectionId),
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

  // Puntos para el mapa
  const mapPoints = visibleSections
    .filter((s) => s.geomGeoJson)
    .map((s) => ({
      id: s.sectionId,
      name: s.roomName,
      geomGeoJson: s.geomGeoJson,
      popupText: `${s.roomName} (${formatDistance(s.distanceMeters).text})`,
    }));

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
                {/* Geolocalización — Integrante 3 */}
                <div className="mb-3 d-flex align-items-center gap-2 flex-wrap">
                  {!position && (
                    <button className="btn btn-outline-primary btn-sm" onClick={requestLocation} disabled={geoLoading}>
                      {geoLoading ? 'Obteniendo ubicación...' : '📍 Compartir mi ubicación'}
                    </button>
                  )}
                  {position && (
                    <span className="badge bg-success">
                      📍 Ubicación: {position.lat.toFixed(5)}, {position.lng.toFixed(5)}
                    </span>
                  )}
                  {geoError && <span className="text-danger small">{geoError}</span>}
                </div>

                {!position && !geoLoading && (
                  <div className="alert alert-secondary py-2 small">
                    Comparte tu ubicación para ver la distancia a cada sección y ordenar por cercanía.
                  </div>
                )}

                {position && (
                  <>
                    {/* Toggle solo cercanas */}
                    <div className="form-check mb-3">
                      <input
                        className="form-check-input" type="checkbox" id="nearbyToggle"
                        checked={showOnlyNearby}
                        onChange={() => setShowOnlyNearby(!showOnlyNearby)}
                      />
                      <label className="form-check-label small" htmlFor="nearbyToggle">
                        Solo mostrar muy cercanas (≤ {NEARBY_THRESHOLD_M}m)
                      </label>
                    </div>

                    {loadingSections && (
                      <div className="text-center py-3">
                        <div className="spinner-border spinner-border-sm text-primary" role="status" />
                      </div>
                    )}

                    {!loadingSections && visibleSections.length === 0 && (
                      <div className="alert alert-info py-2 small">
                        {sections.length === 0
                          ? 'No hay secciones disponibles con cupo para esta asignatura en el semestre activo.'
                          : `No hay secciones a ≤ ${NEARBY_THRESHOLD_M}m. Desactiva el filtro para ver todas.`}
                      </div>
                    )}

                    {visibleSections.length > 0 && (
                      <div className="list-group mb-3">
                        {visibleSections.map((s) => {
                          const { text, isFar } = formatDistance(s.distanceMeters);
                          const isNearby = s.distanceMeters <= NEARBY_THRESHOLD_M;
                          return (
                            <div key={s.sectionId}
                              className={`list-group-item d-flex justify-content-between align-items-center flex-wrap gap-2 ${isFar ? 'bg-danger bg-opacity-10' : ''}`}>
                              <div>
                                <div className="fw-semibold small">
                                  {selectedSubject?.code} — {selectedSubject?.name}
                                </div>
                                <div className="text-muted small">
                                  🚪 {s.roomName} ({s.buildingName}) · Sección #{s.sectionId}
                                </div>
                                <div className="mt-1 d-flex gap-1 flex-wrap">
                                  {isNearby && <span className="badge bg-success">📍 A {Math.round(s.distanceMeters)}m de ti</span>}
                                  {isFar    && <span className="badge bg-danger">⚠️ Lejos ({text})</span>}
                                  {!isNearby && !isFar && <span className="badge bg-secondary">{text}</span>}
                                </div>
                              </div>
                              <button
                                className="btn btn-primary btn-sm"
                                onClick={() => handleEnroll(s.sectionId)}
                                disabled={enrollingId === s.sectionId}>
                                {enrollingId === s.sectionId ? 'Inscribiendo...' : 'Inscribir'}
                              </button>
                            </div>
                          );
                        })}
                      </div>
                    )}

                    {/* Mapa */}
                    {mapPoints.length > 0 && (
                      <div className="mt-3" style={{ height: 300 }}>
                        <MapView
                          center={[position.lat, position.lng]}
                          zoom={17}
                          pendingMarker={[position.lat, position.lng]}
                          rooms={mapPoints}
                        />
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
