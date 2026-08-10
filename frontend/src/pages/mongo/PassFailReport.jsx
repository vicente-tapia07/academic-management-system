import React, { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../../services/api";
import MongoTabs from "./MongoTabs";

/**
 * PassFailReport — Laboratorio 3 · Integrante 4 (Frontend 2)
 *
 * Consume GET /api/mongo/reports/pass-fail-rate?subjectId=&semesterId=
 *
 * MongoReportController devuelve un OBJETO con dos listas, no un arreglo:
 *
 *   {
 *     "bySubjectAndSemester": [ PassFailRateItem, ... ],   // $group + $cond
 *     "gradeDistribution":    [ GradeDistributionBucket ]  // $bucket
 *   }
 *
 * PassFailRateItem:
 *   { subjectId, subjectCode, subjectName, semesterId, semesterYear,
 *     semesterPeriod, totalGraded, approved, failed,
 *     approvalRate, failureRate, averageGrade }
 *
 * GradeDistributionBucket:
 *   { label, rangeStart, rangeEndExclusive, count }
 *
 * Los selectores de asignatura y semestre NO requieren endpoints extra: la
 * primera llamada se hace sin filtros y ReportAggregationService agrega todas
 * las combinaciones existentes en `grades`. Cada fila ya trae los ids y
 * nombres, así que las opciones se deducen de ahí.
 */

const PERIOD_LABEL = {
  "1S": "1er Semestre",
  "2S": "2do Semestre",
  SUMMER: "Verano",
};

const BUCKET_STYLE = {
  "REPROBADO [1.0-4.0)": { bar: "bg-danger", badge: "bg-danger" },
  "SUFICIENTE [4.0-5.0)": { bar: "bg-warning", badge: "bg-warning text-dark" },
  "BUENO [5.0-6.0)": { bar: "bg-info", badge: "bg-info text-dark" },
  "DISTINGUIDO [6.0-7.0]": { bar: "bg-success", badge: "bg-success" },
};

const semesterLabel = (year, period) =>
  `${year} · ${PERIOD_LABEL[period] ?? period}`;

// ApiExceptionHandler responde { status, error }. No usa "message".
const readApiError = (err, fallback) => {
  const data = err?.response?.data;
  if (typeof data === "string" && data.trim()) return data;
  return data?.error || data?.message || fallback;
};

// Mismos cortes de color que ya usa FailureReport.jsx
const failureBarColor = (pct) => {
  if (pct > 60) return "bg-danger";
  if (pct > 40) return "bg-warning";
  if (pct > 20) return "bg-info";
  return "bg-success";
};

export default function PassFailReport() {
  const navigate = useNavigate();

  // Catálogo: resultado de la primera llamada sin filtros. Alimenta los selects
  // y se mantiene fijo aunque después se filtre.
  const [catalog, setCatalog] = useState([]);
  const [catalogLoaded, setCatalogLoaded] = useState(false);

  const [rates, setRates] = useState([]);
  const [distribution, setDistribution] = useState([]);

  const [subjectId, setSubjectId] = useState("");
  const [semesterId, setSemesterId] = useState("");

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [lastRefresh, setLastRefresh] = useState(null);

  const fetchReport = useCallback(async (subject, semester) => {
    setLoading(true);
    setError("");
    try {
      const params = {};
      if (subject) params.subjectId = subject;
      if (semester) params.semesterId = semester;

      const res = await api.get("/api/mongo/reports/pass-fail-rate", {
        params,
      });
      const items = res.data?.bySubjectAndSemester ?? [];
      const buckets = res.data?.gradeDistribution ?? [];

      setRates(items);
      setDistribution(buckets);
      setLastRefresh(new Date().toLocaleTimeString("es-CL"));
      return items;
    } catch (err) {
      setError(
        readApiError(
          err,
          "No se pudo ejecutar el pipeline de agregación en MongoDB.",
        ),
      );
      setRates([]);
      setDistribution([]);
      return [];
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    (async () => {
      const items = await fetchReport("", "");
      setCatalog(items);
      setCatalogLoaded(true);
    })();
  }, [fetchReport]);

  // Opciones únicas derivadas del catálogo
  const subjectOptions = [];
  const seenSubjects = new Set();
  catalog.forEach((item) => {
    if (!seenSubjects.has(item.subjectId)) {
      seenSubjects.add(item.subjectId);
      subjectOptions.push({
        id: item.subjectId,
        code: item.subjectCode,
        name: item.subjectName,
      });
    }
  });
  subjectOptions.sort((a, b) => a.code.localeCompare(b.code));

  const semesterOptions = [];
  const seenSemesters = new Set();
  catalog.forEach((item) => {
    if (!seenSemesters.has(item.semesterId)) {
      seenSemesters.add(item.semesterId);
      semesterOptions.push({
        id: item.semesterId,
        year: item.semesterYear,
        period: item.semesterPeriod,
      });
    }
  });
  semesterOptions.sort(
    (a, b) => a.year - b.year || a.period.localeCompare(b.period),
  );

  const handleSubjectChange = (e) => {
    const value = e.target.value;
    setSubjectId(value);
    fetchReport(value, semesterId);
  };

  const handleSemesterChange = (e) => {
    const value = e.target.value;
    setSemesterId(value);
    fetchReport(subjectId, value);
  };

  const handleReset = () => {
    setSubjectId("");
    setSemesterId("");
    fetchReport("", "");
  };

  const totalGraded = rates.reduce((acc, r) => acc + r.totalGraded, 0);
  const totalApproved = rates.reduce((acc, r) => acc + r.approved, 0);
  const totalFailed = rates.reduce((acc, r) => acc + r.failed, 0);
  const globalApproval =
    totalGraded > 0 ? ((totalApproved / totalGraded) * 100).toFixed(1) : "0.0";
  const distributionTotal = distribution.reduce((acc, b) => acc + b.count, 0);

  return (
    <div className="container py-4">
      <div className="d-flex justify-content-between align-items-start mb-4">
        <button
          className="btn btn-outline-secondary"
          onClick={() => navigate(-1)}
        >
          ← Volver
        </button>
        <div className="text-center">
          <h2 className="fw-bold mb-0">Tasa de Aprobación y Reprobación</h2>
          <p className="text-muted mb-0">
            Aggregation Pipeline: <code>$group</code> (agrupación) +{" "}
            <code>$bucket</code> (clasificación de notas)
          </p>
        </div>
        <div style={{ width: 90 }} />
      </div>

      <MongoTabs />

      {/* ── Filtros ── */}
      <div className="card border-0 shadow-sm mb-4">
        <div className="card-body">
          <div className="row g-3 align-items-end">
            <div className="col-md-5">
              <label className="form-label fw-semibold small">Asignatura</label>
              <select
                className="form-select"
                value={subjectId}
                onChange={handleSubjectChange}
                disabled={!catalogLoaded}
              >
                <option value="">— Todas las asignaturas —</option>
                {subjectOptions.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.code} — {s.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="col-md-4">
              <label className="form-label fw-semibold small">Semestre</label>
              <select
                className="form-select"
                value={semesterId}
                onChange={handleSemesterChange}
                disabled={!catalogLoaded}
              >
                <option value="">— Todos los semestres —</option>
                {semesterOptions.map((s) => (
                  <option key={s.id} value={s.id}>
                    {semesterLabel(s.year, s.period)}
                  </option>
                ))}
              </select>
            </div>

            <div className="col-md-3 d-flex gap-2">
              <button
                className="btn btn-outline-secondary flex-grow-1"
                onClick={handleReset}
                disabled={loading}
              >
                Limpiar filtros
              </button>
              <button
                className="btn btn-outline-primary"
                onClick={() => fetchReport(subjectId, semesterId)}
                disabled={loading}
                title="Volver a ejecutar el pipeline"
              >
                {loading ? "..." : "↻"}
              </button>
            </div>
          </div>

          {lastRefresh && (
            <p className="text-muted small mb-0 mt-2">
              Pipeline ejecutado a las {lastRefresh}
            </p>
          )}
        </div>
      </div>

      {loading && (
        <p className="text-muted">Ejecutando pipeline en MongoDB...</p>
      )}
      {error && <div className="alert alert-danger">{error}</div>}

      {!loading && !error && rates.length === 0 && (
        <div className="alert alert-info">
          No hay calificaciones registradas para los filtros seleccionados.
        </div>
      )}

      {!loading && !error && rates.length > 0 && (
        <>
          {/* ── Tarjetas resumen ── */}
          <div className="row g-3 mb-4">
            <div className="col-6 col-md-3">
              <div className="card border-0 shadow-sm text-center py-3 h-100">
                <div className="fs-2 fw-bold text-primary">{totalGraded}</div>
                <div className="text-muted small">Notas registradas</div>
              </div>
            </div>
            <div className="col-6 col-md-3">
              <div className="card border-0 shadow-sm text-center py-3 h-100">
                <div className="fs-2 fw-bold text-success">{totalApproved}</div>
                <div className="text-muted small">Aprobados</div>
              </div>
            </div>
            <div className="col-6 col-md-3">
              <div className="card border-0 shadow-sm text-center py-3 h-100">
                <div className="fs-2 fw-bold text-danger">{totalFailed}</div>
                <div className="text-muted small">Reprobados</div>
              </div>
            </div>
            <div className="col-6 col-md-3">
              <div className="card border-0 shadow-sm text-center py-3 h-100">
                <div className="fs-2 fw-bold" style={{ color: "#003366" }}>
                  {globalApproval}%
                </div>
                <div className="text-muted small">Tasa de aprobación</div>
              </div>
            </div>
          </div>

          {/* ── Tabla por asignatura y semestre ── */}
          <div className="card shadow-sm border-0 mb-4">
            <div className="card-header bg-white fw-semibold py-3">
              Detalle por asignatura y semestre ({rates.length} combinación
              {rates.length === 1 ? "" : "es"})
            </div>
            <div className="table-responsive">
              <table className="table table-hover mb-0 align-middle">
                <thead className="table-light">
                  <tr>
                    <th>Código</th>
                    <th>Asignatura</th>
                    <th>Semestre</th>
                    <th className="text-center" style={{ width: 80 }}>
                      Notas
                    </th>
                    <th className="text-center" style={{ width: 90 }}>
                      Aprob.
                    </th>
                    <th className="text-center" style={{ width: 90 }}>
                      Reprob.
                    </th>
                    <th style={{ minWidth: 170 }}>Tasa de reprobación</th>
                    <th className="text-end" style={{ width: 90 }}>
                      Promedio
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {rates.map((r) => (
                    <tr key={`${r.subjectId}-${r.semesterId}`}>
                      <td>
                        <span className="badge bg-primary font-monospace">
                          {r.subjectCode}
                        </span>
                      </td>
                      <td className="fw-semibold small">{r.subjectName}</td>
                      <td className="text-muted small text-nowrap">
                        {semesterLabel(r.semesterYear, r.semesterPeriod)}
                      </td>
                      <td className="text-center text-muted">
                        {r.totalGraded}
                      </td>
                      <td className="text-center text-success fw-bold">
                        {r.approved}
                      </td>
                      <td className="text-center text-danger fw-bold">
                        {r.failed}
                      </td>
                      <td>
                        <div className="d-flex align-items-center gap-2">
                          <div
                            className="progress flex-grow-1"
                            style={{ height: 8 }}
                          >
                            <div
                              className={`progress-bar ${failureBarColor(r.failureRate)}`}
                              style={{
                                width: `${Math.min(r.failureRate, 100)}%`,
                              }}
                            />
                          </div>
                          <span
                            className="text-muted small text-nowrap"
                            style={{ width: 46 }}
                          >
                            {r.failureRate.toFixed(1)}%
                          </span>
                        </div>
                      </td>
                      <td className="text-end">
                        <span
                          className={`fw-bold ${r.averageGrade >= 4 ? "text-success" : "text-danger"}`}
                        >
                          {r.averageGrade.toFixed(1)}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* ── Distribución por rango ($bucket) ── */}
          <div className="card shadow-sm border-0">
            <div className="card-header bg-white py-3">
              <span className="fw-semibold">
                Distribución de notas por rango
              </span>
              <span className="text-muted small ms-2">
                Etapa <code>$bucket</code> · {distributionTotal} nota
                {distributionTotal === 1 ? "" : "s"}
              </span>
            </div>
            <div className="card-body">
              {distributionTotal === 0 ? (
                <p className="text-muted mb-0">
                  Sin notas en el rango seleccionado.
                </p>
              ) : (
                distribution.map((bucket) => {
                  const pct =
                    distributionTotal > 0
                      ? (bucket.count / distributionTotal) * 100
                      : 0;
                  const style = BUCKET_STYLE[bucket.label] ?? {
                    bar: "bg-secondary",
                    badge: "bg-secondary",
                  };
                  return (
                    <div key={bucket.label} className="mb-3">
                      <div className="d-flex justify-content-between align-items-center mb-1">
                        <span className={`badge ${style.badge}`}>
                          {bucket.label}
                        </span>
                        <span className="text-muted small">
                          {bucket.count} nota{bucket.count === 1 ? "" : "s"} ·{" "}
                          {pct.toFixed(1)}%
                        </span>
                      </div>
                      <div className="progress" style={{ height: 14 }}>
                        <div
                          className={`progress-bar ${style.bar}`}
                          style={{ width: `${pct}%` }}
                        />
                      </div>
                    </div>
                  );
                })
              )}
            </div>
            <div className="card-footer bg-white text-muted small">
              Rangos calculados en MongoDB con <code>$bucket</code> sobre el
              campo <code>value</code> de la colección <code>grades</code>{" "}
              (escala 1.0 – 7.0, aprobación desde 4.0).
            </div>
          </div>
        </>
      )}
    </div>
  );
}
