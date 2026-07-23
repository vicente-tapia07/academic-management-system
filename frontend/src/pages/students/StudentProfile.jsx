import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';
import MapView from '../../components/MapView';

const statusMap = {
  ACTIVE:    { cls: 'success',   label: 'Activo'     },
  INACTIVE:  { cls: 'secondary', label: 'Inactivo'   },
  SUSPENDED: { cls: 'warning',   label: 'Suspendido' },
  BLOCKED:   { cls: 'danger',    label: 'Bloqueado'  },
  GRADUATED: { cls: 'primary',   label: 'Egresado'   },
};

// Geocodificación inversa: coordenadas → dirección en texto (Nominatim)
async function reverseGeocode(lat, lng) {
  const url = `https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lng}&format=json`;
  const res  = await fetch(url, { headers: { 'Accept-Language': 'es' } });
  const data = await res.json();
  return data?.display_name ?? `${lat.toFixed(5)}, ${lng.toFixed(5)}`;
}

// Convierte una dirección en texto a coordenadas usando Nominatim (OpenStreetMap).
// Es gratuito, sin API key, igual que Leaflet.
async function geocodeAddress(address) {
  const url = `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(address)}&format=json&limit=1&countrycodes=cl`;
  const res = await fetch(url, {
    headers: { 'Accept-Language': 'es' }
  });
  const data = await res.json();
  if (!data || data.length === 0) return null;
  return {
    lat: parseFloat(data[0].lat),
    lng: parseFloat(data[0].lon),
    displayName: data[0].display_name,
  };
}

export default function StudentProfile() {
  const { user }    = useAuth();
  const navigate    = useNavigate();

  const [student,       setStudent]      = useState(null);
  const [loading,       setLoading]      = useState(true);
  const [error,         setError]        = useState('');
  const [savedAddress,  setSavedAddress] = useState('');  // dirección guardada en texto

  // Estado del formulario de ubicación
  const [address,       setAddress]       = useState('');
  const [searchLoading, setSearchLoading] = useState(false);
  const [searchError,   setSearchError]   = useState('');
  const [preview,       setPreview]       = useState(null);
  const [saveLoading,   setSaveLoading]   = useState(false);
  const [saveSuccess,   setSaveSuccess]   = useState('');

  const loadSavedLocation = async () => {
    try {
      const res = await api.get(`/api/students/${user.id}/location`);
      const { latitude, longitude } = res.data;
      const displayName = await reverseGeocode(latitude, longitude);
      setSavedAddress(displayName);
    } catch {
      setSavedAddress(''); // sin ubicación guardada
    }
  };

  useEffect(() => {
    api.get(`/api/students/${user.id}`)
      .then((r) => setStudent(r.data))
      .catch(() => setError('No se pudo cargar el perfil.'))
      .finally(() => setLoading(false));
    loadSavedLocation();
  }, [user.id]);

  // Paso 1: buscar coordenadas de la dirección ingresada
  const handleSearch = async (e) => {
    e.preventDefault();
    if (!address.trim()) return;
    setSearchLoading(true);
    setSearchError('');
    setPreview(null);
    setSaveSuccess('');

    try {
      const result = await geocodeAddress(address);
      if (!result) {
        setSearchError('No se encontró la dirección. Intenta ser más específico (ej: "Av Ecuador 3659, Estación Central, Santiago").');
      } else {
        setPreview(result);
      }
    } catch {
      setSearchError('Error al buscar la dirección. Verifica tu conexión.');
    } finally {
      setSearchLoading(false);
    }
  };

  // Paso 2: confirmar y guardar las coordenadas en el backend
  const handleSaveLocation = async () => {
    if (!preview) return;
    setSaveLoading(true);
    setSaveSuccess('');
    setSearchError('');

    try {
      await api.patch(`/api/students/${user.id}/location`, {
        latitude:  preview.lat,
        longitude: preview.lng,
      });
      setSaveSuccess('✅ Ubicación guardada correctamente.');
      setPreview(null);
      setAddress('');
      loadSavedLocation(); // actualiza la dirección mostrada en el perfil
    } catch {
      setSearchError('Error al guardar la ubicación. Intenta de nuevo.');
    } finally {
      setSaveLoading(false);
    }
  };

  if (loading) return <div className="container py-5 text-muted">Cargando...</div>;
  if (error)   return <div className="container py-5"><div className="alert alert-danger">{error}</div></div>;

  const st = statusMap[student?.academicStatus] ?? { cls: 'secondary', label: student?.academicStatus };

  // Construimos un marcador para el mapa de preview
  const previewBuilding = preview ? [{
    id: 'preview',
    name: preview.displayName,
    geomGeoJson: JSON.stringify({
      type: 'Point',
      coordinates: [preview.lng, preview.lat],
    }),
  }] : [];

  return (
    <div className="container py-4" style={{ maxWidth: 640 }}>

      <div className="d-flex align-items-center gap-3 mb-4">
        <button className="btn btn-sm btn-outline-secondary" onClick={() => navigate('/my-dashboard')}>
          ← Volver
        </button>
        <h2 className="fw-bold mb-0">Mi Perfil</h2>
      </div>

      {/* ── Tarjeta de datos personales ── */}
      <div className="card border-0 shadow-sm mb-4">
        <div className="card-body p-4">

          <div className="text-center mb-4">
            <div
              className="rounded-circle d-inline-flex align-items-center justify-content-center text-white fw-bold"
              style={{ width: 80, height: 80, fontSize: '2rem', backgroundColor: '#003366' }}
            >
              {student?.firstName?.[0]}{student?.lastName?.[0]}
            </div>
            <h4 className="fw-bold mt-3 mb-1">
              {student?.firstName} {student?.lastName}
            </h4>
            <span className={`badge bg-${st.cls}`}>{st.label}</span>
          </div>

          <hr />

          {[
            { label: 'Correo',    value: user.email                },
            { label: 'Matrícula', value: student?.enrollmentNumber },
            { label: 'Estado',    value: st.label                  },
          ].map(({ label, value }) => (
            <div key={label} className="d-flex justify-content-between py-2 border-bottom">
              <span className="text-muted small fw-medium">{label}</span>
              <span className="fw-semibold small">{value}</span>
            </div>
          ))}

          {/* Dirección guardada */}
          <div className="d-flex justify-content-between py-2 border-bottom align-items-start gap-3">
            <span className="text-muted small fw-medium text-nowrap">📍 Dirección</span>
            <span className="fw-semibold small text-end">
              {savedAddress
                ? savedAddress
                : <span className="text-muted fst-italic">Sin dirección registrada</span>}
            </span>
          </div>

        </div>
      </div>

      {/* ── Sección de ubicación ── */}
      <div className="card border-0 shadow-sm">
        <div className="card-body p-4">
          <h5 className="fw-semibold mb-1">📍 Mi dirección de residencia</h5>
          <p className="text-muted small mb-3">
            Tu dirección se usa para el análisis geoespacial del campus.
            Escribe tu dirección y el sistema la convertirá en coordenadas automáticamente.
          </p>

          {saveSuccess && (
            <div className="alert alert-success py-2">{saveSuccess}</div>
          )}

          {/* Formulario de búsqueda */}
          <form onSubmit={handleSearch} className="d-flex gap-2 mb-3">
            <input
              type="text"
              className="form-control"
              placeholder="Ej: Av Ecuador 3659, Estación Central, Santiago"
              value={address}
              onChange={(e) => { setAddress(e.target.value); setSaveSuccess(''); }}
            />
            <button
              type="submit"
              className="btn btn-primary text-nowrap"
              disabled={searchLoading || !address.trim()}
            >
              {searchLoading ? (
                <><span className="spinner-border spinner-border-sm me-1" />Buscando...</>
              ) : '🔍 Buscar'}
            </button>
          </form>

          {searchError && (
            <div className="alert alert-warning py-2 small">{searchError}</div>
          )}

          {/* Preview en el mapa antes de confirmar */}
          {preview && (
            <div className="mt-3">
              <div className="alert alert-info py-2 small mb-3">
                <strong>📌 Encontrado:</strong> {preview.displayName}
                <br />
                <span className="text-muted">
                  Lat: {preview.lat.toFixed(6)}, Lng: {preview.lng.toFixed(6)}
                </span>
              </div>

              {/* Usamos MapView con rooms para mostrar el marcador de punto */}
              <MapView
                rooms={[{
                  id: 'preview',
                  name: '📍 Tu dirección',
                  geomGeoJson: JSON.stringify({
                    type: 'Point',
                    coordinates: [preview.lng, preview.lat],
                  }),
                  popupText: preview.displayName,
                }]}
                center={[preview.lat, preview.lng]}
                zoom={15}
              />

              <p className="text-muted small mt-2 mb-3">
                ¿Es tu dirección correcta? Si el marcador no está en el lugar correcto,
                intenta con una dirección más específica.
              </p>

              <div className="d-flex gap-2">
                <button
                  className="btn btn-success flex-grow-1"
                  onClick={handleSaveLocation}
                  disabled={saveLoading}
                >
                  {saveLoading ? (
                    <><span className="spinner-border spinner-border-sm me-1" />Guardando...</>
                  ) : '✅ Confirmar y guardar ubicación'}
                </button>
                <button
                  className="btn btn-outline-secondary"
                  onClick={() => { setPreview(null); setAddress(''); }}
                  disabled={saveLoading}
                >
                  Cancelar
                </button>
              </div>
            </div>
          )}

        </div>
      </div>

    </div>
  );
}
