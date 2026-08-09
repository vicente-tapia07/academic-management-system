import React, { useCallback, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import api from "../../services/api";
import MongoTabs from "./MongoTabs";

/**
 * StudentCertificate — Laboratorio 3 · Integrante 4 (Frontend 2)
 *
 * Consume GET /api/mongo/certificates/{studentId}
 *
 * El endpoint no calcula nada al momento de la petición: lee la colección
 * materializada `certificados_notas`, que CertificateChangeStreamService
 * mantiene al día de forma reactiva escuchando `grades` con Change Streams y
 * republicando el documento del estudiante mediante $merge.
 *
 * Forma de la respuesta, derivada del $group del servicio:
 *
 *   {
 *     "_id": "<ObjectId del estudiante en hexadecimal>",
 *     "entries": [
 *       { "subjectCode", "subjectName", "semesterYear",
 *         "semesterPeriod", "grade", "recordedAt" }
 *     ],
 *     "promedioGeneral": 5.3,
 *     "totalRamos": 6,
 *     "updatedAt": "2026-08-09T14:22:31Z"
 *   }
 *
 * El estudiante se elige desde un selector poblado con
 * GET /api/mongo/directory/students (MongoStudentDirectoryController), porque
 * el resto de la API de Mongo exige conocer el ObjectId de antemano.
 *
 * Si ese endpoint todavía no está desplegado, la vista degrada de forma
 * elegante a un campo manual de ObjectId en lugar de quedar inutilizable.
 */

const OBJECT_ID_PATTERN = /^[0-9a-fA-F]{24}$/;
const PASSING_GRADE = 4.0;

const PERIOD_LABEL = { "1S": "1er Semestre", "2S": "2do Semestre", SUMMER: "Verano" };

// ApiExceptionHandler responde { status, error }; no usa "message".
const readApiError = (err, fallback) => {
  const data = err?.response?.data;
  if (typeof data === "string" && data.trim()) return data;
  return data?.error || data?.message || fallback;
};

const formatDate = (isoString) => {
  if (!isoString) return "—";
  const date = new Date(isoString);
  if (Number.isNaN(date.getTime())) return "—";
  return date.toLocaleDateString("es-CL", { day: "2-digit", month: "2-digit", year: "numeric" });
};

const formatDateTime = (isoString) => {
  if (!isoString) return "—";
  const date = new Date(isoString);
  if (Number.isNaN(date.getTime())) return "—";
  return date.toLocaleString("es-CL");
};

export default function StudentCertificate() {
  const navigate = useNavigate();
  const { studentId: studentIdFromUrl } = useParams();

  // Directorio de estudiantes
  const [directory, setDirectory] = useState([]);
  const [directoryReady, setDirectoryReady] = useState(false);
  const [directoryFailed, setDirectoryFailed] = useState(false);

  const [selectedId, setSelectedId] = useState(studentIdFromUrl ?? "");
  const [manualId, setManualId] = useState("");

  const [certificate, setCertificate] = useState(null);
  const [student, setStudent] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  // ── Cargar el directorio al montar ──
  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const res = await api.get("/api/mongo/directory/students");
        if (cancelled) return;
        setDirectory(res.data ?? []);
        setDirectoryFailed(false);
      } catch {
        if (cancelled) return;
        setDirectory([]);
        setDirectoryFailed(true);
      } finally {
        if (!cancelled) setDirectoryReady(true);
      }
    })();
    return () => { cancelled = true; };
  }, []);

  const loadCertificate = useCallback(async (rawId) => {
    const id = (rawId ?? "").trim();

    if (!OBJECT_ID_PATTERN.test(id)) {
      setError("El identificador debe ser un ObjectId de MongoDB: 24 caracteres hexadecimales.");
      setCertificate(null);
      setStudent(null);
      return;
    }

    setLoading(true);
    setError("");
    setCertificate(null);
    setStudent(null);

    try {
      const res = await api.get(`/api/mongo/certificates/${id}`);
      setCertificate(res.data);
    } catch (err) {
      if (err.response?.status === 404) {
        setError(
          "Este estudiante todavía no tiene certificado. La colección materializada " +
          "certificados_notas solo incluye a quienes tienen al menos una calificación registrada."
        );
      } else {
        setError(readApiError(err, "No se pudo obtener el certificado desde MongoDB."));
      }
      setLoading(false);
      return;
    }

    // Datos personales: primero desde el directorio ya cargado (no gasta una
    // petición). Si no está, se intenta el endpoint de Backend 1, que exige
    // rol ADMIN, dentro de un try/catch silencioso.
    const fromDirectory = directory.find((s) => s.id === id);
    if (fromDirectory) {
      setStudent(fromDirectory);
    } else {
      try {
        const studentRes = await api.get(`/api/mongo/students/${id}`);
        setStudent(studentRes.data);
      } catch {
        setStudent(null);
      }
    }

    setLoading(false);
  }, [directory]);

  // Carga automática cuando el id viene en la URL. Espera al directorio para
  // poder resolver el nombre sin una petición extra.
  useEffect(() => {
    if (studentIdFromUrl && directoryReady) {
      setSelectedId(studentIdFromUrl);
      loadCertificate(studentIdFromUrl);
    }
  }, [studentIdFromUrl, directoryReady, loadCertificate]);

  const handleSelectChange = (e) => {
    const id = e.target.value;
    setSelectedId(id);
    setError("");
    if (!id) {
      setCertificate(null);
      setStudent(null);
      return;
    }
    navigate(`/mongo/certificate/${id}`);
    loadCertificate(id);
  };

  const handleManualSubmit = (e) => {
    e.preventDefault();
    const id = manualId.trim();
    if (OBJECT_ID_PATTERN.test(id)) navigate(`/mongo/certificate/${id}`);
    loadCertificate(id);
  };

  // Agrupación por semestre. El backend ya ordena por año, período y código
  // antes del $group, así que basta con recorrer las entradas en orden.
  const entries = certificate?.entries ?? [];
  const groupedEntries = [];
  entries.forEach((entry) => {
    const key = `${entry.semesterYear}-${entry.semesterPeriod}`;
    let group = groupedEntries.find((g) => g.key === key);
    if (!group) {
      group = { key, year: entry.semesterYear, period: entry.semesterPeriod, rows: [] };
      groupedEntries.push(group);
    }
    group.rows.push(entry);
  });

  const approvedCount = entries.filter((e) => e.grade >= PASSING_GRADE).length;
  const failedCount = entries.length - approvedCount;

  const withCertificate = directory.filter((s) => s.hasCertificate);
  const withoutCertificate = directory.filter((s) => !s.hasCertificate);

  return (
    <div className="container py-4">
      {/* Al imprimir queda visible únicamente la hoja del certificado */}
      <style>{`
        @media print {
          body * { visibility: hidden; }
          .certificate-sheet, .certificate-sheet * { visibility: visible; }
          .certificate-sheet {
            position: absolute; left: 0; top: 0; width: 100%;
            box-shadow: none !important; border: none !important;
          }
          .no-print { display: none !important; }
        }
      `}</style>

      <div className="d-flex justify-content-between align-items-start mb-4 no-print">
        <button className="btn btn-outline-secondary" onClick={() => navigate(-1)}>← Volver</button>
        <div className="text-center">
          <h2 className="fw-bold mb-0">Certificado de Notas</h2>
          <p className="text-muted mb-0">
            Colección materializada <code>certificados_notas</code> · Change Streams + <code>$merge</code>
          </p>
        </div>
        <div style={{ width: 90 }} />
      </div>

      <div className="no-print"><MongoTabs /></div>

      {/* ── Selección del estudiante ── */}
      <div className="card border-0 shadow-sm mb-4 no-print">
        <div className="card-body">

          {!directoryReady && (
            <p className="text-muted mb-0">Cargando directorio de estudiantes...</p>
          )}

          {directoryReady && !directoryFailed && (
            <>
              <label className="form-label fw-semibold small" htmlFor="student-select">
                Estudiante
              </label>
              <select id="student-select" className="form-select"
                value={selectedId} onChange={handleSelectChange}>
                <option value="">— Selecciona un estudiante —</option>

                {withCertificate.length > 0 && (
                  <optgroup label="Con certificado disponible">
                    {withCertificate.map((s) => (
                      <option key={s.id} value={s.id}>
                        {s.lastName}, {s.firstName} · {s.enrollmentNumber}
                      </option>
                    ))}
                  </optgroup>
                )}

                {withoutCertificate.length > 0 && (
                  <optgroup label="Sin calificaciones registradas">
                    {withoutCertificate.map((s) => (
                      <option key={s.id} value={s.id}>
                        {s.lastName}, {s.firstName} · {s.enrollmentNumber}
                      </option>
                    ))}
                  </optgroup>
                )}
              </select>

              <div className="form-text">
                {directory.length} estudiante{directory.length === 1 ? "" : "s"} en el
                directorio · {withCertificate.length} con certificado materializado.
              </div>

              {directory.length === 0 && (
                <div className="alert alert-warning py-2 small mt-3 mb-0">
                  La colección <code>students</code> está vacía. Ejecuta el seed de MongoDB
                  (<code>database/mongo/3_seed_mongo.js</code>) antes de continuar.
                </div>
              )}
            </>
          )}

          {directoryReady && directoryFailed && (
            <>
              <div className="alert alert-warning py-2 small">
                No se pudo cargar el directorio de estudiantes. Verifica que el endpoint{" "}
                <code>GET /api/mongo/directory/students</code> esté desplegado. Mientras
                tanto puedes ingresar el ObjectId manualmente.
              </div>
              <form onSubmit={handleManualSubmit}>
                <label className="form-label fw-semibold small">
                  Identificador del estudiante (ObjectId)
                </label>
                <div className="input-group">
                  <span className="input-group-text font-monospace">_id</span>
                  <input type="text" className="form-control font-monospace"
                    placeholder="507f1f77bcf86cd799439011"
                    value={manualId} maxLength={24}
                    onChange={(e) => { setManualId(e.target.value); setError(""); }} />
                  <button className="btn btn-primary" type="submit"
                    disabled={loading || manualId.trim().length === 0}>
                    {loading ? "Buscando..." : "Ver certificado"}
                  </button>
                </div>
              </form>
            </>
          )}

        </div>
      </div>

      {error && <div className="alert alert-danger no-print">{error}</div>}
      {loading && <p className="text-muted no-print">Consultando la vista materializada...</p>}

      {/* ── Certificado ── */}
      {certificate && !loading && (
        <>
          <div className="d-flex justify-content-end mb-2 no-print">
            <button className="btn btn-outline-primary btn-sm" onClick={() => window.print()}>
              🖨️ Imprimir / Guardar como PDF
            </button>
          </div>

          <div className="card shadow-sm border-0 certificate-sheet">
            <div className="card-body p-4 p-md-5">

              {/* Encabezado formal */}
              <div className="text-center border-bottom pb-3 mb-4">
                <div style={{ fontSize: "2rem" }}>🎓</div>
                <h3 className="fw-bold mb-1" style={{ color: "#003366" }}>
                  Universidad de Santiago de Chile
                </h3>
                <p className="text-muted mb-0">Certificado de Concentración de Notas</p>
              </div>

              {/* Datos del estudiante */}
              <div className="row g-3 mb-4">
                <div className="col-md-6">
                  <div className="text-muted small">Estudiante</div>
                  <div className="fw-semibold">
                    {student ? `${student.firstName} ${student.lastName}` : "Nombre no disponible"}
                  </div>
                </div>
                <div className="col-md-3">
                  <div className="text-muted small">N° de matrícula</div>
                  <div className="fw-semibold font-monospace">{student?.enrollmentNumber ?? "—"}</div>
                </div>
                <div className="col-md-3">
                  <div className="text-muted small">Carrera</div>
                  <div className="fw-semibold">{student?.careerCode ?? "—"}</div>
                </div>
                <div className="col-12">
                  <div className="text-muted small">Identificador interno</div>
                  <div className="font-monospace small">{certificate._id}</div>
                </div>
              </div>

              {/* Resumen */}
              <div className="row g-3 mb-4">
                <div className="col-6 col-md-3">
                  <div className="border rounded text-center py-3">
                    <div className="fs-4 fw-bold" style={{ color: "#003366" }}>
                      {certificate.totalRamos ?? entries.length}
                    </div>
                    <div className="text-muted small">Ramos cursados</div>
                  </div>
                </div>
                <div className="col-6 col-md-3">
                  <div className="border rounded text-center py-3">
                    <div className="fs-4 fw-bold text-success">{approvedCount}</div>
                    <div className="text-muted small">Aprobados</div>
                  </div>
                </div>
                <div className="col-6 col-md-3">
                  <div className="border rounded text-center py-3">
                    <div className="fs-4 fw-bold text-danger">{failedCount}</div>
                    <div className="text-muted small">Reprobados</div>
                  </div>
                </div>
                <div className="col-6 col-md-3">
                  <div className="border rounded text-center py-3">
                    <div className={`fs-4 fw-bold ${
                      (certificate.promedioGeneral ?? 0) >= PASSING_GRADE ? "text-success" : "text-danger"
                    }`}>
                      {certificate.promedioGeneral != null
                        ? Number(certificate.promedioGeneral).toFixed(1)
                        : "—"}
                    </div>
                    <div className="text-muted small">Promedio general</div>
                  </div>
                </div>
              </div>

              {/* Detalle por semestre */}
              {groupedEntries.length === 0 && (
                <p className="text-muted">El certificado no contiene calificaciones registradas.</p>
              )}

              {groupedEntries.map((group) => (
                <div key={group.key} className="mb-4">
                  <h6 className="fw-semibold mb-2" style={{ color: "#003366" }}>
                    {group.year} · {PERIOD_LABEL[group.period] ?? group.period}
                  </h6>
                  <div className="table-responsive">
                    <table className="table table-sm table-bordered mb-0 align-middle">
                      <thead className="table-light">
                        <tr>
                          <th style={{ width: 110 }}>Código</th>
                          <th>Asignatura</th>
                          <th style={{ width: 130 }}>Fecha de registro</th>
                          <th className="text-center" style={{ width: 90 }}>Nota</th>
                          <th className="text-center" style={{ width: 110 }}>Situación</th>
                        </tr>
                      </thead>
                      <tbody>
                        {group.rows.map((row, index) => {
                          const approved = row.grade >= PASSING_GRADE;
                          return (
                            <tr key={`${group.key}-${row.subjectCode}-${index}`}>
                              <td className="font-monospace small">{row.subjectCode}</td>
                              <td>{row.subjectName}</td>
                              <td className="text-muted small">{formatDate(row.recordedAt)}</td>
                              <td className="text-center">
                                <span className={`fw-bold ${approved ? "text-success" : "text-danger"}`}>
                                  {Number(row.grade).toFixed(1)}
                                </span>
                              </td>
                              <td className="text-center">
                                <span className={`badge ${approved ? "bg-success" : "bg-danger"}`}>
                                  {approved ? "Aprobado" : "Reprobado"}
                                </span>
                              </td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  </div>
                </div>
              ))}

              {/* Pie */}
              <div className="border-top pt-3 mt-4 text-muted small">
                <div className="d-flex justify-content-between flex-wrap gap-2">
                  <span>
                    Documento generado desde la colección materializada <code>certificados_notas</code>.
                  </span>
                  <span>Última actualización: {formatDateTime(certificate.updatedAt)}</span>
                </div>
                <p className="mb-0 mt-2">
                  La colección se actualiza automáticamente mediante Change Streams cada vez que se
                  registra o modifica una calificación; no requiere refresco manual.
                </p>
              </div>

            </div>
          </div>
        </>
      )}
    </div>
  );
}
