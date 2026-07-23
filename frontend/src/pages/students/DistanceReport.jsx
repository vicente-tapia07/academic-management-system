import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import useGeolocation from '../../hooks/useGeolocation';
import MapView from '../../components/MapView';

const DistanceReport = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const { position, error: geoError, loading: geoLoading, requestLocation } = useGeolocation();

  const [subjects, setSubjects] = useState([]);
  const [selectedSubjectId, setSelectedSubjectId] = useState('');
  const [sections, setSections] = useState([]);
  const [filteredSections, setFilteredSections] = useState([]);
  const [showOnlyNearby, setShowOnlyNearby] = useState(false);
  const [loading, setLoading] = useState(false);
  const [enrolling, setEnrolling] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Función para formatear distancia y determinar si es lejos
  const formatDistance = (meters) => {
    if (meters >= 1000) {
      return { text: `${(meters / 1000).toFixed(1)} km`, isFar: true };
    }
    return { text: `${Math.round(meters)} m`, isFar: meters > 300 };
  };

  // Cargar asignaturas disponibles al montar
  useEffect(() => {
    const fetchSubjects = async () => {
      try {
        const res = await api.get('/api/subjects');
        setSubjects(res.data);
        if (res.data.length > 0) {
          setSelectedSubjectId(res.data[0].id);
        }
      } catch (err) {
        setError('Error al cargar las asignaturas.');
      }
    };
    fetchSubjects();
  }, []);

  // Solicitar ubicación al montar (opcional)
  useEffect(() => {
    requestLocation();
  }, []);

  // Cuando cambia la asignatura o la ubicación, buscar secciones
  useEffect(() => {
    if (selectedSubjectId && position) {
      fetchNearbySections(selectedSubjectId, position.lat, position.lng);
    } else if (selectedSubjectId && !position) {
      setSections([]);
      setFilteredSections([]);
    }
  }, [selectedSubjectId, position]);

  const fetchNearbySections = async (subjectId, lat, lng) => {
    setLoading(true);
    setError('');
    try {
      const res = await api.get('/api/enrollments/nearby-sections', {
        params: { subjectId, lat, lng },
      });
      setSections(res.data);
      setFilteredSections(res.data);
    } catch (err) {
      setError('Error al cargar secciones.');
    } finally {
      setLoading(false);
    }
  };

  // Aplicar filtro de "solo cercanas" (≤ 150m)
  useEffect(() => {
    if (showOnlyNearby) {
      setFilteredSections(sections.filter(sec => sec.distanceMeters <= 150));
    } else {
      setFilteredSections(sections);
    }
  }, [showOnlyNearby, sections]);

  const handleSubjectChange = (e) => {
    setSelectedSubjectId(e.target.value);
    setSuccess('');
    setError('');
  };

  const handleToggle = () => setShowOnlyNearby(!showOnlyNearby);

  const handleEnroll = async (sectionId) => {
    if (!sectionId) {
      setError('Selecciona una sección para inscribirte.');
      return;
    }
    setEnrolling(true);
    setError('');
    setSuccess('');
    try {
      await api.post('/api/enrollments/enroll', {
        studentId: user.id,
        sectionId: sectionId,
      });
      setSuccess('¡Inscripción exitosa!');
      if (position) {
        fetchNearbySections(selectedSubjectId, position.lat, position.lng);
      }
    } catch (err) {
      const msg = err.response?.data?.message || 'Error al inscribirte.';
      setError(msg);
    } finally {
      setEnrolling(false);
    }
  };

  // Preparar puntos para el mapa (secciones con geometría)
  const mapPoints = filteredSections
    .filter(sec => sec.geomGeoJson)
    .map(sec => ({
      id: sec.sectionId,
      name: sec.roomName,
      geomGeoJson: sec.geomGeoJson,
      popupText: `${sec.roomName} (${formatDistance(sec.distanceMeters).text})`,
    }));

  return (
    <div className="container py-4">
      <h2 className="fw-bold mb-3">Reporte de Distancias - Inscripción Inteligente</h2>
      <p className="text-muted">
        Selecciona una asignatura y te mostraremos todas las secciones disponibles con su distancia.
      </p>

      {/* Selector de asignatura */}
      <div className="mb-3">
        <label className="form-label fw-semibold">Asignatura</label>
        <select
          className="form-select"
          value={selectedSubjectId}
          onChange={handleSubjectChange}
        >
          {subjects.map(sub => (
            <option key={sub.id} value={sub.id}>
              {sub.code} - {sub.name}
            </option>
          ))}
        </select>
      </div>

      {/* Geolocalización */}
      <div className="mb-3">
        {!position && (
          <button
            className="btn btn-outline-primary"
            onClick={requestLocation}
            disabled={geoLoading}
          >
            {geoLoading ? 'Obteniendo ubicación...' : '📍 Compartir mi ubicación'}
          </button>
        )}
        {position && (
          <span className="badge bg-success">
            📍 Ubicación: {position.lat.toFixed(5)}, {position.lng.toFixed(5)}
          </span>
        )}
        {geoError && <div className="text-danger small mt-1">{geoError}</div>}
      </div>

      {/* Toggle y lista de secciones */}
      {position && (
        <>
          <div className="form-check mb-3">
            <input
              className="form-check-input"
              type="checkbox"
              id="nearbyToggle"
              checked={showOnlyNearby}
              onChange={handleToggle}
            />
            <label className="form-check-label" htmlFor="nearbyToggle">
              Solo mostrar muy cercanas (≤ 150m)
            </label>
          </div>

          {loading && <div className="spinner-border text-primary" role="status"><span className="visually-hidden">Cargando...</span></div>}

          {!loading && filteredSections.length === 0 && (
            <div className="alert alert-info">
              {sections.length === 0
                ? 'No hay secciones disponibles para esta asignatura en el semestre actual.'
                : 'No hay secciones que cumplan con el filtro de distancia (≤ 150m). Desactiva el filtro para ver todas.'}
            </div>
          )}

          {filteredSections.length > 0 && (
            <div className="list-group mb-3">
              {filteredSections.map((sec) => {
                const { text, isFar } = formatDistance(sec.distanceMeters);
                return (
                  <div
                    key={sec.sectionId}
                    className={`list-group-item list-group-item-action d-flex justify-content-between align-items-center ${isFar ? 'bg-danger bg-opacity-10' : ''}`}
                  >
                    <div>
                      <strong>{sec.roomName}</strong>
                      <span className="text-muted ms-2">({sec.buildingName})</span>
                      {sec.distanceMeters <= 150 && (
                        <span className="badge bg-success ms-2">⭐ A 150m de ti</span>
                      )}
                      {isFar && (
                        <span className="badge bg-danger ms-2">⚠️ Lejos</span>
                      )}
                      <div className="small text-muted">
                        Código: {sec.sectionCode} · Distancia: {text}
                      </div>
                    </div>
                    <button
                      className="btn btn-primary btn-sm"
                      onClick={() => handleEnroll(sec.sectionId)}
                      disabled={enrolling}
                    >
                      Inscribir
                    </button>
                  </div>
                );
              })}
            </div>
          )}

          {error && <div className="alert alert-danger">{error}</div>}
          {success && <div className="alert alert-success">{success}</div>}

          {/* Mapa de las secciones */}
          {mapPoints.length > 0 && position && (
            <div className="mt-3" style={{ height: '300px' }}>
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

      {!position && !geoLoading && (
        <div className="alert alert-secondary">
          Comparte tu ubicación para ver las secciones cercanas.
        </div>
      )}
    </div>
  );
};

export default DistanceReport;