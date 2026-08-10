import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../../services/api";
import { useAuth } from "../../context/AuthContext";

export default function StudentCertificate() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [cert, setCert] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const load = async () => {
      try {
        const studentsRes = await api.get("/api/students");
        const me = studentsRes.data.find((s) => s.usuarioId === user.id);
        if (!me) {
          setError("No se encontró tu perfil de estudiante.");
          return;
        }
        const certRes = await api.get(`/api/certificates/${me.id}`);
        setCert(certRes.data);
      } catch (err) {
        setError(
          err.response?.status === 404
            ? "Aún no tienes un certificado de notas disponible. Registra tus calificaciones primero."
            : "Error al cargar tu certificado de notas.",
        );
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [user.id]);

  const totalEarned = (cert?.totalRamos ?? 0) * 1000;

  if (loading) return <p className="text-muted p-4">Cargando certificado...</p>;
  if (error) return <div className="alert alert-danger m-4">{error}</div>;

  return (
    <div className="container py-4">
      <button className="btn btn-outline-secondary mb-3" onClick={() => navigate(-1)}>
        ← Volver
      </button>
      <div className="text-center mb-4">
        <h2 className="fw-bold mb-1">Certificado de Notas</h2>
        <p className="text-muted mb-0">
          Vista materializada mantenida con Change Streams + $merge (tarea 6)
        </p>
      </div>

      <div className="card shadow-sm border-0 mb-4">
        <div className="card-body">
          <div className="row text-center">
            <div className="col-4">
              <div className="text-muted small text-uppercase">Ramos cursados</div>
              <div className="fw-bold fs-3">{cert.totalRamos}</div>
            </div>
            <div className="col-4">
              <div className="text-muted small text-uppercase">Promedio general</div>
              <div className="fw-bold fs-3 text-primary">
                {cert.promedioGeneral.toFixed(1)}
              </div>
            </div>
            <div className="col-4">
              <div className="text-muted small text-uppercase">Créditos acumulados</div>
              <div className="fw-bold fs-3">{totalEarned}</div>
            </div>
          </div>
          <div className="text-center text-muted small mt-3">
            Actualizado: {new Date(cert.updatedAt).toLocaleString("es-CL")}
          </div>
        </div>
      </div>

      <div className="card shadow-sm border-0">
        <div className="card-header bg-white fw-semibold py-3">
          Detalle por asignatura y semestre
        </div>
        <div className="table-responsive">
          <table className="table table-hover mb-0 align-middle">
            <thead className="table-light">
              <tr>
                <th>Semestre</th>
                <th>Código</th>
                <th>Asignatura</th>
                <th className="text-end">Nota</th>
                <th>Resultado</th>
              </tr>
            </thead>
            <tbody>
              {cert.entries.map((e, i) => (
                <tr key={`${e.subjectCode}-${e.semesterYear}-${i}`}>
                  <td className="text-nowrap text-muted">
                    {e.semesterYear}-{e.semesterPeriod}
                  </td>
                  <td>
                    <span className="badge bg-primary font-monospace">{e.subjectCode}</span>
                  </td>
                  <td className="fw-semibold">{e.subjectName}</td>
                  <td className="text-end">
                    <span
                      className={`fw-bold fs-5 ${
                        e.grade >= 4.0 ? "text-success" : "text-danger"
                      }`}
                    >
                      {Number(e.grade).toFixed(1)}
                    </span>
                  </td>
                  <td>
                    <span className={`badge ${e.grade >= 4.0 ? "bg-success" : "bg-danger"}`}>
                      {e.grade >= 4.0 ? "Aprobado" : "Reprobado"}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
