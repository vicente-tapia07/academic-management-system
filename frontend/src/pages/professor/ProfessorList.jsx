import React, { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import api from "../../services/api";

export default function ProfessorList() {
  const navigate = useNavigate();
  const [professors, setProfessors] = useState([]);
  const [filtered,   setFiltered]   = useState([]);
  const [search,     setSearch]     = useState('');
  const [loading,    setLoading]    = useState(true);
  const [error,      setError]      = useState('');

  const load = () => {
    setLoading(true);
    api.get('/api/professors')
      .then((r) => { setProfessors(r.data); setFiltered(r.data); })
      .catch(() => setError('Error al cargar los profesores.'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  useEffect(() => {
    const q = search.toLowerCase();
    setFiltered(professors.filter((p) =>
      p.firstName.toLowerCase().includes(q) ||
      p.lastName.toLowerCase().includes(q)  ||
      p.department.toLowerCase().includes(q)
    ));
  }, [search, professors]);

  const handleDelete = async (id, name) => {
    if (!window.confirm(`¿Eliminar al profesor "${name}"?`)) return;
    try {
      await api.delete(`/api/professors/${id}`);
      load();
    } catch (err) {
      alert(err.response?.data || 'Error al eliminar el profesor.');
    }
  };

  return (
    <div className="container py-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div className="d-flex align-items-center gap-3">
          <button className="btn btn-outline-secondary" onClick={() => navigate('/dashboard')}>
            ← Volver
          </button>
          <div>
            <h2 className="fw-bold mb-0">Profesores</h2>
            <p className="text-muted mb-0">Listado de profesores registrados</p>
          </div>
        </div>
        <Link to="/professors/new" className="btn btn-primary">+ Nuevo Profesor</Link>
      </div>

      <div className="mb-3" style={{ maxWidth: 360 }}>
        <input type="text" className="form-control"
          placeholder="Buscar por nombre o departamento..."
          value={search} onChange={(e) => setSearch(e.target.value)} />
      </div>

      {loading && <p className="text-muted">Cargando...</p>}
      {error   && <div className="alert alert-danger">{error}</div>}

      {!loading && !error && (
        <>
          <p className="text-muted small mb-2">{filtered.length} profesor(es)</p>
          <div className="card shadow-sm border-0">
            <div className="table-responsive">
              <table className="table table-hover mb-0 align-middle">
                <thead className="table-light">
                  <tr>
                    <th>Nombre</th>
                    <th>Apellido</th>
                    <th>Departamento</th>
                    <th className="text-end">Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {filtered.length === 0 && (
                    <tr><td colSpan={4} className="text-center text-muted py-4">
                      No se encontraron profesores
                    </td></tr>
                  )}
                  {filtered.map((p) => (
                    <tr key={p.id}>
                      <td className="fw-semibold">{p.firstName}</td>
                      <td>{p.lastName}</td>
                      <td className="text-muted small">{p.department}</td>
                      <td className="text-end d-flex gap-1 justify-content-end">
                        <Link to={`/professors/${p.id}/courses`}
                          className="btn btn-sm btn-outline-info">
                          Cursos
                        </Link>
                        <Link to={`/professors/edit/${p.id}`}
                          className="btn btn-sm btn-outline-primary">
                          Editar
                        </Link>
                        <button className="btn btn-sm btn-outline-danger"
                          onClick={() => handleDelete(p.id, `${p.firstName} ${p.lastName}`)}>
                          Eliminar
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
