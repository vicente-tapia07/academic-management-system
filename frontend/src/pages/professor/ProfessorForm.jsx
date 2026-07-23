import React, { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import api from "../../services/api";

export default function ProfessorForm() {
  const navigate = useNavigate();
  const { id }   = useParams();
  const isEdit   = Boolean(id);

  const [form, setForm] = useState({
    name: "", rut: "", email: "", password: "", department: "",
  });
  const [loading, setLoading] = useState(false);
  const [error,   setError]   = useState('');

  useEffect(() => {
    if (!isEdit) return;
    api.get(`/api/professors/${id}`).then((r) => {
      setForm({
        name:       `${r.data.firstName} ${r.data.lastName}`.trim(),
        rut:        '',
        email:      '',
        password:   '',
        department: r.data.department ?? '',
      });
    });
  }, [id, isEdit]);

  const handleChange = (e) =>
    setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      if (isEdit) {
        await api.put(`/api/professors/${id}`, form);
      } else {
        await api.post('/api/professors', form);
      }
      navigate('/professors');
    } catch (err) {
      setError(
        err.response?.data?.message ||
        err.response?.data ||
        'Error al guardar el profesor.'
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container py-4" style={{ maxWidth: 500 }}>
      <div className="d-flex align-items-center gap-3 mb-4">
        <button className="btn btn-outline-secondary"
          onClick={() => navigate('/professors')}>← Volver</button>
        <h2 className="fw-bold mb-0">
          {isEdit ? 'Editar Profesor' : 'Nuevo Profesor'}
        </h2>
      </div>

      {error && <div className="alert alert-danger">{error}</div>}

      <div className="card shadow-sm border-0">
        <div className="card-body p-4">
          <form onSubmit={handleSubmit}>
            <div className="row g-3">

              <div className="col-12">
                <label className="form-label fw-semibold">Nombre completo</label>
                <input className="form-control" name="name"
                  value={form.name} onChange={handleChange}
                  placeholder="Carlos Ruiz" required />
              </div>

              <div className="col-12">
                <label className="form-label fw-semibold">Departamento</label>
                <input className="form-control" name="department"
                  value={form.department} onChange={handleChange}
                  placeholder="Informática" required />
              </div>

              {!isEdit && (
                <>
                  <div className="col-sm-5">
                    <label className="form-label fw-semibold">RUT</label>
                    <input className="form-control" name="rut"
                      value={form.rut} onChange={handleChange}
                      placeholder="12345678-9" required />
                  </div>

                  <div className="col-sm-7">
                    <label className="form-label fw-semibold">Email</label>
                    <input type="email" className="form-control" name="email"
                      value={form.email} onChange={handleChange}
                      placeholder="carlos@usach.cl" required />
                  </div>

                  <div className="col-12">
                    <label className="form-label fw-semibold">Contraseña</label>
                    <input type="password" className="form-control" name="password"
                      value={form.password} onChange={handleChange}
                      placeholder="••••••••" required />
                  </div>
                </>
              )}

            </div>

            <div className="d-flex gap-2 mt-4">
              <button type="submit" className="btn btn-primary flex-grow-1"
                disabled={loading}>
                {loading ? 'Guardando...' : isEdit ? 'Guardar Cambios' : 'Crear Profesor'}
              </button>
              <button type="button" className="btn btn-outline-secondary"
                onClick={() => navigate('/professors')}>
                Cancelar
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
