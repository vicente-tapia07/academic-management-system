import React, { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../../services/api";
import MongoTabs from "./MongoTabs";

/**
 * SubjectSearch — Laboratorio 3 · Integrante 4 (Frontend 2)
 *
 * Consume GET /api/mongo/subjects/search?q=texto
 *
 * ReportAggregationService.searchSubjects() resuelve la consulta con el índice
 * de texto `text_subject_name` (creado en 2_indexes.js sobre subjects.name, con
 * default_language "spanish"). Si el stemmer no encuentra coincidencias por un
 * tema de tildes, reintenta con una expresión regular insensible a acentos.
 *
 * Respuesta: List<SubjectSummary>
 *   [{ "id", "code", "name", "credits", "careerCode", "active" }]
 *
 * Detalles de implementación:
 *  - El backend lanza IllegalArgumentException si `q` viene vacío, así que
 *    nunca se dispara la petición con una cadena en blanco.
 *  - La búsqueda es en vivo con 350 ms de espera (debounce) para no lanzar una
 *    consulta por cada tecla pulsada.
 *  - Cada petición lleva un número de secuencia: si una respuesta lenta llega
 *    después de otra más reciente, se descarta en lugar de pisar el resultado.
 */

const DEBOUNCE_MS = 350;

const readApiError = (err, fallback) => {
  const data = err?.response?.data;
  if (typeof data === "string" && data.trim()) return data;
  return data?.error || data?.message || fallback;
};

export default function SubjectSearch() {
  const navigate = useNavigate();

  const [query, setQuery] = useState("");
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [hasSearched, setHasSearched] = useState(false);

  const requestSeq = useRef(0);

  useEffect(() => {
    const trimmed = query.trim();

    if (trimmed.length === 0) {
      setResults([]);
      setError("");
      setLoading(false);
      setHasSearched(false);
      return;
    }

    setLoading(true);
    const seq = ++requestSeq.current;

    const timer = setTimeout(async () => {
      try {
        const res = await api.get("/api/mongo/subjects/search", { params: { q: trimmed } });
        if (seq !== requestSeq.current) return; // respuesta obsoleta
        setResults(res.data ?? []);
        setError("");
      } catch (err) {
        if (seq !== requestSeq.current) return;
        setResults([]);
        setError(readApiError(err, "No se pudo ejecutar la búsqueda en MongoDB."));
      } finally {
        if (seq === requestSeq.current) {
          setLoading(false);
          setHasSearched(true);
        }
      }
    }, DEBOUNCE_MS);

    return () => clearTimeout(timer);
  }, [query]);

  return (
    <div className="container py-4">
      <div className="d-flex justify-content-between align-items-start mb-4">
        <button className="btn btn-outline-secondary" onClick={() => navigate(-1)}>← Volver</button>
        <div className="text-center">
          <h2 className="fw-bold mb-0">Buscador del Catálogo</h2>
          <p className="text-muted mb-0">
            Índice de texto <code>text_subject_name</code> sobre <code>subjects.name</code>
          </p>
        </div>
        <div style={{ width: 90 }} />
      </div>

      <MongoTabs />

      {/* ── Campo de búsqueda ── */}
      <div className="card border-0 shadow-sm mb-4">
        <div className="card-body">
          <label className="form-label fw-semibold small">Buscar asignatura por nombre</label>
          <div className="input-group input-group-lg">
            <span className="input-group-text">🔎</span>
            <input type="text" className="form-control"
              placeholder="programación, bases de datos, cálculo..."
              value={query} onChange={(e) => setQuery(e.target.value)} autoFocus />
            {query && (
              <button className="btn btn-outline-secondary" type="button"
                onClick={() => setQuery("")} title="Limpiar">✕</button>
            )}
          </div>
          <div className="form-text">
            La búsqueda se ejecuta automáticamente mientras escribes. Tolera palabras sin tilde.
          </div>
        </div>
      </div>

      {loading && <p className="text-muted">Consultando el índice de texto...</p>}
      {error && <div className="alert alert-danger">{error}</div>}

      {!loading && !error && query.trim().length === 0 && (
        <div className="alert alert-secondary">
          Escribe el nombre (o parte del nombre) de una asignatura para consultar el catálogo.
        </div>
      )}

      {!loading && !error && hasSearched && results.length === 0 && (
        <div className="alert alert-info">
          No se encontraron asignaturas que coincidan con <strong>«{query.trim()}»</strong>.
        </div>
      )}

      {!loading && !error && results.length > 0 && (
        <>
          <p className="text-muted small mb-2">
            {results.length} asignatura{results.length === 1 ? "" : "s"} encontrada
            {results.length === 1 ? "" : "s"}
          </p>

          <div className="row g-3">
            {results.map((subject) => (
              <div className="col-md-6" key={subject.id}>
                <div className="card border-0 shadow-sm h-100">
                  <div className="card-body">
                    <div className="d-flex justify-content-between align-items-start mb-2">
                      <span className="badge bg-primary font-monospace fs-6">{subject.code}</span>
                      <span className={`badge ${subject.active ? "bg-success" : "bg-secondary"}`}>
                        {subject.active ? "Vigente" : "No vigente"}
                      </span>
                    </div>

                    <h5 className="fw-bold mb-2">{subject.name}</h5>

                    <div className="d-flex flex-wrap gap-3 text-muted small mb-2">
                      <span>🎓 Carrera: {subject.careerCode}</span>
                      <span>📘 {subject.credits} crédito{subject.credits === 1 ? "" : "s"}</span>
                    </div>

                    <div className="text-muted font-monospace" style={{ fontSize: "0.72rem" }}>
                      _id: {subject.id}
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  );
}
