import React, { useEffect, useState } from "react";
import { useParams, useNavigate, Link } from "react-router-dom";
import api from "../../services/api";

export default function StudentEnrollmentsAdmin() {
  const { id } = useParams(); // ID del estudiante
  const navigate = useNavigate();

  const [student, setStudent] = useState(null);
  const [enrollments, setEnrollments] = useState([]);
  const [sections, setSections] = useState([]);
  const [subjects, setSubjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadData = async () => {
    setLoading(true);
    try {
      const [stuRes, enrRes, secRes, subRes] = await Promise.all([
        api.get(`/api/students/${id}`),
        api.get(`/api/enrollments/student/${id}`),
        api.get("/api/sections"),
        api.get("/api/subjects"),
      ]);
      setStudent(stuRes.data);
      setEnrollments(enrRes.data);
      setSections(secRes.data);
      setSubjects(subRes.data);
    } catch {
      setError("Error al cargar la información del estudiante.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [id]);

  const getSubjectName = (sectionId) => {
    const sec = sections.find((s) => s.id === sectionId);
    if (!sec) return `Sección #${sectionId}`;
    const sub = subjects.find((s) => s.id === sec.subjectId);
    return sub ? `${sub.code} - ${sub.name}` : `Asignatura #${sec.subjectId}`;
  };

  // Cambiar estado (ACTIVE, APPROVED, REPROBATED)
  const handleStatusChange = async (enrollmentId, newStatus) => {
    try {
      await api.patch(`/api/enrollments/${enrollmentId}/status`, newStatus, {
        headers: { "Content-Type": "text/plain" },
      });
      loadData();
    } catch {
      alert("Error al actualizar el estado de la asignatura.");
    }
  };

  // Cancelar/Eliminar inscripción (libera cupo)
  const handleCancel = async (enrollmentId) => {
    if (
      !window.confirm(
        "¿Cancelar esta inscripción? Se eliminará del registro y se devolverá el cupo a la sección.",
      )
    )
      return;
    try {
      await api.delete(`/api/enrollments/${enrollmentId}`);
      loadData();
    } catch {
      alert("Error al cancelar la inscripción.");
    }
  };

  if (loading) return <p className="text-muted p-4">Cargando cursos...</p>;

  return (
    <div className="container py-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div className="d-flex align-items-center gap-3">
          <button
            className="btn btn-outline-secondary"
            onClick={() => navigate("/students")}
          >
            ← Volver a Estudiantes
          </button>
          <div>
            <h2 className="fw-bold mb-0">Cursos en Cursado</h2>
            <p className="text-muted mb-0">
              Estudiante:{" "}
              {student
                ? `${student.firstName} ${student.lastName} (${student.enrollmentNumber})`
                : `#${id}`}
            </p>
          </div>
        </div>
        <Link to="/students/enroll" className="btn btn-primary">
          + Inscribir en nueva sección
        </Link>
      </div>

      {error && <div className="alert alert-danger">{error}</div>}

      <div className="card shadow-sm border-0">
        <div className="table-responsive">
          <table className="table table-hover align-middle mb-0">
            <thead className="table-light">
              <tr>
                <th>Asignatura / Sección</th>
                <th>Fecha Inscripción</th>
                <th>Estado Actual</th>
                <th>Cambiar Estado</th>
                <th className="text-end">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {enrollments.length === 0 && (
                <tr>
                  <td colSpan={5} className="text-center text-muted py-4">
                    Este estudiante no tiene asignaturas inscritas actualmente.
                  </td>
                </tr>
              )}
              {enrollments.map((e) => (
                <tr key={e.id}>
                  <td className="fw-semibold">
                    {getSubjectName(e.sectionId)}
                    <span className="text-muted small d-block">
                      Sección ID: #{e.sectionId}
                    </span>
                  </td>
                  <td>{e.enrollmentDate || "—"}</td>
                  <td>
                    <span
                      className={`badge ${
                        e.status === "ACTIVE"
                          ? "bg-success"
                          : e.status === "APPROVED"
                            ? "bg-primary"
                            : e.status === "REPROBATED"
                              ? "bg-danger"
                              : "bg-secondary"
                      }`}
                    >
                      {e.status}
                    </span>
                  </td>
                  <td>
                    <select
                      className="form-select form-select-sm"
                      style={{ width: 140 }}
                      value={e.status}
                      onChange={(ev) =>
                        handleStatusChange(e.id, ev.target.value)
                      }
                    >
                      <option value="ACTIVE">En Cursado</option>
                      <option value="APPROVED">Aprobado</option>
                      <option value="REPROBATED">Reprobado</option>
                    </select>
                  </td>
                  <td className="text-end">
                    <button
                      className="btn btn-sm btn-outline-danger"
                      onClick={() => handleCancel(e.id)}
                    >
                      Sacar de Sección
                    </button>
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
