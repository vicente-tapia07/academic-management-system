import React, { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import api from "../../services/api";

export default function ProfessorForm() {
  const navigate = useNavigate();
  const { id } = useParams();
  const isEdit = Boolean(id);

  const [form, setForm] = useState({
    name: "",
    email: "",
    password: "",
    department: "",
  });

  useEffect(() => {
    if (isEdit) {
      api.get(`/api/professors/${id}`).then((r) => {
        setForm({
          name: `${r.data.firstName} ${r.data.lastName}`,
          email: "",
          password: "",
          department: r.data.department,
        });
      });
    }
  }, [id, isEdit]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (isEdit) {
        await api.put(`/api/professors/${id}`, form);
      } else {
        await api.post("/api/professors", form);
      }
      navigate("/professors");
    } catch {
      alert("Error al guardar el profesor.");
    }
  };

  return (
    <div className="container py-4" style={{ maxWidth: 500 }}>
      <h2 className="fw-bold mb-3">
        {isEdit ? "Editar Profesor" : "Nuevo Profesor"}
      </h2>
      <form onSubmit={handleSubmit} className="card p-4 shadow-sm border-0">
        <div className="mb-3">
          <label className="form-label fw-semibold">Nombre Completo</label>
          <input
            className="form-control"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            required
          />
        </div>
        <div className="mb-3">
          <label className="form-label fw-semibold">Departamento</label>
          <input
            className="form-control"
            value={form.department}
            onChange={(e) => setForm({ ...form, department: e.target.value })}
            required
          />
        </div>
        {!isEdit && (
          <>
            <div className="mb-3">
              <label className="form-label fw-semibold">
                Correo de Usuario
              </label>
              <input
                type="email"
                className="form-control"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                required
              />
            </div>
            <div className="mb-3">
              <label className="form-label fw-semibold">Contraseña</label>
              <input
                type="password"
                className="form-control"
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
                required
              />
            </div>
          </>
        )}
        <div className="d-flex gap-2 mt-2">
          <button type="submit" className="btn btn-primary flex-grow-1">
            Guardar
          </button>
          <button
            type="button"
            className="btn btn-outline-secondary"
            onClick={() => navigate("/professors")}
          >
            Cancelar
          </button>
        </div>
      </form>
    </div>
  );
}
