import React, { useEffect, useState } from "react";
import api from "../../services/api";
import MapView from "../../components/MapView";

/**
 * DensityHeatmap
 *
 * Muestra un mapa coroplético de densidad estudiantil activa por edificio.
 * Consume GET /api/reports/density-heatmap que lee la vista materializada
 * mv_student_density_by_building.
 *
 * Los edificios con más estudiantes aparecen en azul oscuro,
 * los de menos en azul claro.
 */

// Convierte un número de estudiantes en un color hexadecimal.
// maxCount: el máximo del edificio con más estudiantes (para normalizar).
// Si el edificio tiene 0 estudiantes → gris.
// Si tiene el máximo → azul oscuro (#003366, el azul USACH).
// Valores intermedios → interpolación.
function countToColor(count, maxCount) {
  if (maxCount === 0 || count === 0) return "#adb5bd"; // gris Bootstrap

  // ratio va de 0.0 (poco) a 1.0 (máximo)
  const ratio = count / maxCount;

  // Interpolamos entre azul claro (#5b9bd5) y azul oscuro (#003366)
  const r = Math.round(91 + (0 - 91) * ratio); // 91  → 0
  const g = Math.round(155 + (51 - 155) * ratio); // 155 → 51
  const b = Math.round(213 + (102 - 213) * ratio); // 213 → 102

  return `rgb(${r},${g},${b})`;
}

export default function DensityHeatmap() {
  const [data, setData] = useState([]); // lista de DensityHeatmapDTO
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const load = async () => {
    setLoading(true);
    setError("");
    try {
      const res = await api.get("/api/reports/density-heatmap");
      setData(res.data);
    } catch {
      setError("Error al cargar el mapa de densidad estudiantil.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  // Máximo de estudiantes entre todos los edificios (para calcular el color)
  const maxCount =
    data.length > 0 ? Math.max(...data.map((d) => d.studentCount)) : 0;

  // Transformación: DensityHeatmapDTO → formato que espera MapView
  // Tu API devuelve: { buildingId, buildingCode, buildingName, geomJson, studentCount }
  // MapView espera:  { id, name, geomGeoJson, color }
  const buildingsForMap = data.map((d) => ({
    id: d.buildingId,
    name: `${d.buildingName} (${d.studentCount} estudiantes)`,
    geomGeoJson: d.geomJson, // renombramos geomJson → geomGeoJson
    color: countToColor(d.studentCount, maxCount),
  }));

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <div>
          <h5 className="fw-semibold mb-0">
            🗺️ Densidad Estudiantil por Edificio
          </h5>
          <p className="text-muted small mb-0">
            Estudiantes con inscripciones activas · Vista materializada
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
          {/* Mapa principal */}
          <MapView buildings={buildingsForMap} />

          {/* Leyenda de colores */}
          <div className="d-flex align-items-center gap-3 mt-2 flex-wrap">
            <span className="text-muted small">Densidad:</span>
            <span className="d-flex align-items-center gap-1 small">
              <span
                style={{
                  display: "inline-block",
                  width: 16,
                  height: 16,
                  backgroundColor: "#adb5bd",
                  borderRadius: 3,
                }}
              />
              Sin estudiantes
            </span>
            <span className="d-flex align-items-center gap-1 small">
              <span
                style={{
                  display: "inline-block",
                  width: 16,
                  height: 16,
                  backgroundColor: "#5b9bd5",
                  borderRadius: 3,
                }}
              />
              Pocos
            </span>
            <span className="d-flex align-items-center gap-1 small">
              <span
                style={{
                  display: "inline-block",
                  width: 16,
                  height: 16,
                  backgroundColor: "#003366",
                  borderRadius: 3,
                }}
              />
              Muchos
            </span>
          </div>

          {/* Tabla resumen */}
          <div className="mt-4">
            <h6 className="fw-semibold mb-2">Resumen por edificio</h6>
            <div className="table-responsive">
              <table className="table table-sm table-hover mb-0">
                <thead className="table-light">
                  <tr>
                    <th>Edificio</th>
                    <th>Código</th>
                    <th style={{ width: 120 }}>Estudiantes activos</th>
                    <th style={{ minWidth: 180 }}>Densidad relativa</th>
                  </tr>
                </thead>
                <tbody>
                  {data.length === 0 && (
                    <tr>
                      <td colSpan={4} className="text-center text-muted py-3">
                        Sin datos en la vista materializada
                      </td>
                    </tr>
                  )}
                  {data.map((d) => {
                    const pct =
                      maxCount > 0
                        ? Math.round((d.studentCount / maxCount) * 100)
                        : 0;
                    return (
                      <tr key={d.buildingId}>
                        <td className="fw-semibold">{d.buildingName}</td>
                        <td>
                          <span className="badge bg-primary font-monospace">
                            {d.buildingCode}
                          </span>
                        </td>
                        <td className="text-center fw-bold">
                          {d.studentCount}
                        </td>
                        <td>
                          <div className="d-flex align-items-center gap-2">
                            <div
                              className="progress flex-grow-1"
                              style={{ height: 8 }}
                            >
                              <div
                                className="progress-bar"
                                style={{
                                  width: `${pct}%`,
                                  backgroundColor: countToColor(
                                    d.studentCount,
                                    maxCount,
                                  ),
                                }}
                              />
                            </div>
                            <span
                              className="text-muted small"
                              style={{ width: 35 }}
                            >
                              {pct}%
                            </span>
                          </div>
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
