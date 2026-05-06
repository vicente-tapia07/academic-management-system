import React, { useEffect, useState } from 'react';
import api from '../../services/api';

const ALERT_THRESHOLD = 40; // % de reprobación para alertar

export default function FailureReport() {
  const [reports, setReports] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState('');
  const [lastRefresh, setLastRefresh] = useState(null);

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await api.get('/api/professors/reports');
      // Ordenar de mayor a menor tasa de reprobación
      const sorted = [...res.data].sort((a, b) => b.failurePercentage - a.failurePercentage);
      setReports(sorted);
      setLastRefresh(new Date().toLocaleTimeString('es-CL'));
    } catch {
      setError('Error al cargar el reporte de reprobación.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const alerts  = reports.filter((r) => r.failurePercentage > ALERT_THRESHOLD);
  const ok      = reports.filter((r) => r.failurePercentage <= ALERT_THRESHOLD);

  const getBarColor = (pct) => {
    if (pct > 60) return 'bg-danger';
    if (pct > 40) return 'bg-warning';
    if (pct > 20) return 'bg-info';
    return 'bg-success';
  };

  return (
    <div className="container py-4">
      {/* Header */}
      <div className="d-flex justify-content-between align-items-start mb-4">
        <button className="btn btn-outline-secondary me-3" onClick={() => window.history.back()}>
          ← Volver
        </button>
        <div>
          <h2 className="fw-bold mb-0">Reporte de Reprobación</h2>
          <p className="text-muted mb-0">
            Tasa histórica por asignatura · Vista materializada
            {lastRefresh && <span className="ms-2 small">— Actualizado: {lastRefresh}</span>}
          </p>
        </div>
        <button className="btn btn-outline-secondary btn-sm" onClick={load} disabled={loading}>
          {loading ? '...' : '↻ Actualizar'}
        </button>
      </div>

      {loading && <p className="text-muted">Cargando reporte...</p>}
      {error   && <div className="alert alert-danger">{error}</div>}

      {!loading && !error && (
        <>
          {/* Alertas críticas */}
          {alerts.length > 0 && (
            <div className="alert alert-danger d-flex align-items-start gap-2 mb-4">
              <span className="fs-5">⚠️</span>
              <div>
                <strong>{alerts.length} asignatura(s) con más del {ALERT_THRESHOLD}% de reprobación:</strong>
                <ul className="mb-0 mt-1">
                  {alerts.map((r) => (
                    <li key={r.subjectId}>
                      <strong>{r.subjectCode}</strong> — {r.subjectName}:&nbsp;
                      <strong>{r.failurePercentage.toFixed(1)}%</strong>
                      &nbsp;({r.failedGrades}/{r.totalGrades} reprobados)
                    </li>
                  ))}
                </ul>
              </div>
            </div>
          )}

          {alerts.length === 0 && (
            <div className="alert alert-success mb-4">
              ✅ Ninguna asignatura supera el {ALERT_THRESHOLD}% de reprobación.
            </div>
          )}

          {/* Tabla completa */}
          <div className="card shadow-sm border-0">
            <div className="card-header bg-white fw-semibold py-3">
              Todas las asignaturas ({reports.length})
            </div>
            <div className="table-responsive">
              <table className="table table-hover mb-0 align-middle">
                <thead className="table-light">
                  <tr>
                    <th>Código</th>
                    <th>Asignatura</th>
                    <th style={{ width: 90 }}>Total</th>
                    <th style={{ width: 110 }}>Reprobados</th>
                    <th style={{ minWidth: 200 }}>Tasa de Reprobación</th>
                    <th style={{ width: 80 }} className="text-end">%</th>
                  </tr>
                </thead>
                <tbody>
                  {reports.length === 0 && (
                    <tr>
                      <td colSpan={6} className="text-center text-muted py-4">
                        Sin datos en la vista materializada
                      </td>
                    </tr>
                  )}
                  {reports.map((r) => {
                    const pct = r.failurePercentage ?? 0;
                    const isAlert = pct > ALERT_THRESHOLD;
                    return (
                      <tr key={r.subjectId} className={isAlert ? 'table-danger' : ''}>
                        <td>
                          <span className="badge bg-primary font-monospace">{r.subjectCode}</span>
                        </td>
                        <td className="fw-semibold">{r.subjectName}</td>
                        <td className="text-muted">{r.totalGrades}</td>
                        <td>
                          <span className={r.failedGrades > 0 ? 'text-danger fw-bold' : 'text-muted'}>
                            {r.failedGrades}
                          </span>
                        </td>
                        <td>
                          <div className="d-flex align-items-center gap-2">
                            <div className="progress flex-grow-1" style={{ height: 8 }}>
                              <div
                                className={`progress-bar ${getBarColor(pct)}`}
                                style={{ width: `${Math.min(pct, 100)}%` }}
                              />
                            </div>
                          </div>
                        </td>
                        <td className="text-end">
                          <span className={`fw-bold ${isAlert ? 'text-danger' : 'text-muted'}`}>
                            {pct.toFixed(1)}%
                          </span>
                          {isAlert && <span className="ms-1">⚠️</span>}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            {/* Leyenda */}
            <div className="card-footer bg-white text-muted small d-flex gap-3 flex-wrap">
              <span><span className="badge bg-success me-1"> </span>0–20%</span>
              <span><span className="badge bg-info me-1"> </span>21–40%</span>
              <span><span className="badge bg-warning me-1"> </span>41–60%</span>
              <span><span className="badge bg-danger me-1"> </span>&gt;60%</span>
              <span className="ms-auto">⚠️ Alerta cuando supera el {ALERT_THRESHOLD}%</span>
            </div>
          </div>
        </>
      )}
    </div>
  );
}