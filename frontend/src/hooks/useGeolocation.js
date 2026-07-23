import { useState, useCallback } from 'react';

export default function useGeolocation() {
  const [position, setPosition] = useState(null); // { lat, lng }
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const requestLocation = useCallback(() => {
    if (!navigator.geolocation) {
      setError('Tu navegador no soporta geolocalización.');
      return;
    }
    setLoading(true);
    setError('');
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setPosition({
          lat: pos.coords.latitude,
          lng: pos.coords.longitude,
        });
        setLoading(false);
      },
      (err) => {
        setError(
          err.code === err.PERMISSION_DENIED
            ? 'Debes permitir el acceso a tu ubicación para usar esta función.'
            : 'No se pudo obtener tu ubicación. Intenta de nuevo.'
        );
        setLoading(false);
      },
      { enableHighAccuracy: true, timeout: 10000 }
    );
  }, []);

  return { position, error, loading, requestLocation };
}