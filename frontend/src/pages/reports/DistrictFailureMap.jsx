import React, { useEffect, useState } from "react";
import api from "../../services/api";
import MapView from "../../components/MapView";

/**
 * DistrictFailureMap
 *
 * Muestra un mapa coroplético de tasa de reprobación por distrito de vivienda
 * y asignatura. Consume GET /api/reports/failure-by-district que lee la vista
 * materializada mv_failure_rate_by_district.
 *
 * El mapa colorea cada distrito según su tasa de reprobación promedio.
 * La tabla muestra el detalle por distrito y asignatura.
 */

const ALERT_THRESHOLD = 40; // igual que el FailureReport existente

// Convierte un porcentaje de reprobación en un color.
// 0%  → verde (#198754)
// 40% → amarillo (#ffc107)
// 100% → rojo (#dc3545)
function failureToColor(percentage) {
  if (percentage <= 0) return "#198754"; // verde  Bootstrap success
  if (percentage <= 20) return "#20c997"; // teal
  if (percentage <= 40) return "#ffc107"; // amarillo Bootstrap warning
  if (percentage <= 60) return "#fd7e14"; // naranja Bootstrap
  return "#dc3545"; // rojo Bootstrap danger
}

// Agrupa las filas por distrito y calcula el promedio de reprobación.
// Necesario porque la API devuelve una fila por (distrito, asignatura).
// Para colorear el mapa necesitamos un solo valor por distrito.
function groupByDistrict(rows) {
  const map = {};
  rows.forEach((row) => {
    if (!map[row.districtId]) {
      map[row.districtId] = {
        districtId: row.districtId,
        districtName: row.districtName,
        geomJson: row.geomJson,
        subjects: [],
        totalGrades: 0,
        failedGrades: 0,
      };
    }
    map[row.districtId].subjects.push(row);
    map[row.districtId].totalGrades += row.totalGrades;
    map[row.districtId].failedGrades += row.failedGrades;
  });

  // Calculamos el porcentaje global del distrito
  return Object.values(map).map((d) => ({
    ...d,
    failurePercentage:
      d.totalGrades > 0
        ? Math.round((d.failedGrades / d.totalGrades) * 100 * 100) / 100
        : 0,
  }));
}

export default function DistrictFailureMap() {
  const [data, setData] = useState([]); // filas crudas de la API
  const [districts, setDistricts] = useState([]); // agrupadas por distrito
  const [selectedId, setSelectedId] = useState(null); // distrito seleccionado en la tabla
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const load = async () => {
    setLoading(true);
    setError("");
    try {
      const res = await api.get("/api/reports/failure-by-district");
      setData(res.data);
      setDistricts(groupByDistrict(res.data));
    } catch {
      setError("Error al cargar el mapa de reprobación por distrito.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  // Transformación: datos agrupados → formato que espera MapView
  // MapView espera: { id, name, geomGeoJson, color }
  const districtsForMap = districts.map((d) => ({
    id: d.districtId,
    name: `${d.districtName}: ${d.failurePercentage.toFixed(1)}% reprobación`,
    geomGeoJson: d.geomJson, // renombramos geomJson → geomGeoJson
    color: failureToColor(d.failurePercentage),
  }));

  // Filas de detalle para el distrito seleccionado (o todos si no hay selección)
  const detailRows = selectedId
    ? data.filter((r) => r.districtId === selectedId)
    : data;

  const alerts = districts.filter((d) => d.failurePercentage > ALERT_THRESHOLD);

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <div>
          <h5 className="fw-semibold mb-0">
            🏘️ Reprobación por Distrito de Vivienda
          </h5>
          <p className="text-muted small mb-0">
            Tasa de reprobación por zona geográfica · Vista materializada
          </p>
        </div>
        <button
          className="btn btn-outline-secondary btn-sm"
          onClick={load}
          disabled={loading}
        >
          {loading ? "..." : "↻ Actualizar"}
        </button>
      </div>

      {loading && <p className="text-muted">Cargando mapa...</p>}
      {error && <div className="alert alert-danger">{error}</div>}

      {!loading && !error && (
        <>
          {/* Alerta si hay distritos críticos */}
          {alerts.length > 0 && (
            <div className="alert alert-danger d-flex align-items-start gap-2 mb-3">
              <span className="fs-5">⚠️</span>
              <div>
                <strong>
                  {alerts.length} distrito(s) con más del {ALERT_THRESHOLD}% de
                  reprobación promedio:
                </strong>
                <ul className="mb-0 mt-1">
                  {alerts.map((d) => (
                    <li key={d.districtId}>
                      <strong>{d.districtName}</strong>:{" "}
                      {d.failurePercentage.toFixed(1)}% ({d.failedGrades}/
                      {d.totalGrades} reprobados)
                    </li>
                  ))}
                </ul>
              </div>
            </div>
          )}

          {/* Mapa principal */}
          <MapView buildings={districtsForMap} />

          {/* Leyenda */}
          <div className="d-flex align-items-center gap-3 mt-2 flex-wrap">
            <span className="text-muted small">Reprobación:</span>
            {[
              { color: "#198754", label: "0%" },
              { color: "#20c997", label: "1–20%" },
              { color: "#ffc107", label: "21–40%" },
              { color: "#fd7e14", label: "41–60%" },
              { color: "#dc3545", label: ">60%" },
            ].map(({ color, label }) => (
              <span
                key={label}
                className="d-flex align-items-center gap-1 small"
              >
                <span
                  style={{
                    display: "inline-block",
                    width: 16,
                    height: 16,
                    backgroundColor: color,
                    borderRadius: 3,
                  }}
                />
                {label}
              </span>
            ))}
          </div>

          {/* Tabla de distritos con selector */}
          <div className="mt-4">
            <div className="d-flex justify-content-between align-items-center mb-2">
              <h6 className="fw-semibold mb-0">
                Detalle por asignatura
                {selectedId && (
                  <span className="text-muted fw-normal ms-2 small">
                    —{" "}
                    {
                      districts.find((d) => d.districtId === selectedId)
                        ?.districtName
                    }
                  </span>
                )}
              </h6>
              {selectedId && (
                <button
                  className="btn btn-sm btn-outline-secondary"
                  onClick={() => setSelectedId(null)}
                >
                  Ver todos los distritos
                </button>
              )}
            </div>

            {/* Resumen por distrito (clickeable para filtrar) */}
            <div className="row g-2 mb-3">
              {districts.map((d) => {
                const isSelected = selectedId === d.districtId;
                const pct = d.failurePercentage;
                return (
                  <div key={d.districtId} className="col-12 col-sm-6 col-lg-4">
                    <button
                      className={`btn w-100 text-start p-3 border ${
                        isSelected
                          ? "border-primary bg-primary bg-opacity-10"
                          : "border-light bg-white"
                      }`}
                      style={{ borderRadius: 8 }}
                      onClick={() =>
                        setSelectedId(isSelected ? null : d.districtId)
                      }
                    >
                      <div className="d-flex justify-content-between align-items-center mb-1">
                        <span className="fw-semibold small">
                          {d.districtName}
                        </span>
                        <span
                          className="badge"
                          style={{ backgroundColor: failureToColor(pct) }}
                        >
                          {pct.toFixed(1)}%
                        </span>
                      </div>
                      <div className="progress" style={{ height: 6 }}>
                        <div
                          className="progress-bar"
                          style={{
                            width: `${Math.min(pct, 100)}%`,
                            backgroundColor: failureToColor(pct),
                          }}
                        />
                      </div>
                      <div className="text-muted small mt-1">
                        {d.failedGrades} reprobados / {d.totalGrades} notas
                      </div>
                    </button>
                  </div>
                );
              })}
            </div>

            {/* Tabla de detalle */}
            <div className="table-responsive">
              <table className="table table-sm table-hover mb-0">
                <thead className="table-light">
                  <tr>
                    <th>Distrito</th>
                    <th>Asignatura</th>
                    <th style={{ width: 80 }}>Total</th>
                    <th style={{ width: 100 }}>Reprobados</th>
                    <th style={{ minWidth: 160 }}>Tasa</th>
                    <th style={{ width: 70 }} className="text-end">
                      %
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {detailRows.length === 0 && (
                    <tr>
                      <td colSpan={6} className="text-center text-muted py-3">
                        Sin datos en la vista materializada
                      </td>
                    </tr>
                  )}
                  {detailRows.map((r, i) => {
                    const pct = r.failurePercentage ?? 0;
                    const isAlert = pct > ALERT_THRESHOLD;
                    return (
                      <tr
                        key={`${r.districtId}-${r.subjectId}-${i}`}
                        className={isAlert ? "table-danger" : ""}
                      >
                        <td className="text-muted small">{r.districtName}</td>
                        <td>
                          <span className="badge bg-primary font-monospace me-1">
                            {r.subjectCode}
                          </span>
                          <span className="fw-semibold">{r.subjectName}</span>
                        </td>
                        <td className="text-muted">{r.totalGrades}</td>
                        <td>
                          <span
                            className={
                              r.failedGrades > 0
                                ? "text-danger fw-bold"
                                : "text-muted"
                            }
                          >
                            {r.failedGrades}
                          </span>
                        </td>
                        <td>
                          <div className="progress" style={{ height: 7 }}>
                            <div
                              className="progress-bar"
                              style={{
                                width: `${Math.min(pct, 100)}%`,
                                backgroundColor: failureToColor(pct),
                              }}
                            />
                          </div>
                        </td>
                        <td className="text-end">
                          <span
                            className={`fw-bold small ${isAlert ? "text-danger" : "text-muted"}`}
                          >
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
          </div>
        </>
      )}
    </div>
  );
}
