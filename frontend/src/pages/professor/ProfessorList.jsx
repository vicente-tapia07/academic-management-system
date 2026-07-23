import React, { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import api from "../../services/api";

export default function ProfessorList() {
  const navigate = useNavigate();
  const [professors, setProfessors] = useState([]);
  const [loading, setLoading] = useState(true);

  const loadData = () => {
    setLoading(true);
    api
      .get("/api/professors")
      .then((r) => setProfessors(r.data))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleDelete = async (id, name) => {
    if (!window.confirm(`¿Eliminar al profesor ${name}?`)) return;
    try {
      await api.delete(`/api/professors/${id}`);
      loadData();
    } catch {
      alert("Error al eliminar el profesor.");
    }
  };

  return (
    <div className="container py-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <button
          className="btn btn-outline-secondary"
          onClick={() => navigate("/dashboard")}
        >
          ← Volver
        </button>
        <h2 className="fw-bold mb-0">Gestión de Profesores</h2>
        <Link to="/professors/new" className="btn btn-primary">
          + Nuevo Profesor
        </Link>
      </div>

      {loading ? (
        <p className="text-muted">Cargando...</p>
      ) : (
        <div className="card shadow-sm border-0">
          <table className="table table-hover align-middle mb-0">
            <thead className="table-light">
              <tr>
                <th>Nombre</th>
                <th>Departamento</th>
                <th className="text-end">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {professors.map((p) => (
                <tr key={p.id}>
                  <td className="fw-semibold">
                    {p.firstName} {p.lastName}
                  </td>
                  <td>{p.department}</td>
                  <td className="text-end">
                    <Link
                      to={`/professors/edit/${p.id}`}
                      className="btn btn-sm btn-outline-secondary me-2"
                    >
                      Editar
                    </Link>
                    <button
                      className="btn btn-sm btn-outline-danger"
                      onClick={() =>
                        handleDelete(p.id, `${p.firstName} ${p.lastName}`)
                      }
                    >
                      Eliminar
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
