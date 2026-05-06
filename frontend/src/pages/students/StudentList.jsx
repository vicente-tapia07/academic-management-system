import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../../services/api';

const statusBadge = (status) => {
  const map = {
    ACTIVE:      { cls: 'bg-success', label: 'Activo' },
    INACTIVE:    { cls: 'bg-secondary', label: 'Inactivo' },
    SUSPENDED:   { cls: 'bg-warning text-dark', label: 'Suspendido' },
    GRADUATED:   { cls: 'bg-primary', label: 'Egresado' },
  };
  const s = map[status] ?? { cls: 'bg-secondary', label: status };
  return <span className={`badge ${s.cls}`}>{s.label}</span>;
};

export default function StudentList() {
  const [students, setStudents] = useState([]);
  const [filtered, setFiltered] = useState([]);
  const [search, setSearch]     = useState('');
  const [loading, setLoading]   = useState(true);
  const [error, setError]       = useState('');

  useEffect(() => {
    api.get('/api/students')
      .then((r) => { setStudents(r.data); setFiltered(r.data); })
      .catch(() => setError('Error al cargar los estudiantes.'))
      .finally(() => setLoading(false));
  }, []);

  // Filtro en vivo por nombre o número de matrícula
  useEffect(() => {
    const q = search.toLowerCase();
    setFiltered(
      students.filter(
        (s) =>
          s.firstName.toLowerCase().includes(q) ||
          s.lastName.toLowerCase().includes(q) ||
          s.enrollmentNumber.toLowerCase().includes(q)
      )
    );
  }, [search, students]);

  return (
    <div className="container py-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h2 className="fw-bold mb-0">Estudiantes</h2>
          <p className="text-muted mb-0">Listado de alumnos registrados en el sistema</p>
        </div>
      </div>

      {/* Buscador */}
      <div className="mb-3" style={{ maxWidth: 360 }}>
        <input
          type="text"
          className="form-control"
          placeholder="Buscar por nombre o matrícula..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      {loading && <p className="text-muted">Cargando...</p>}
      {error   && <div className="alert alert-danger">{error}</div>}

      {!loading && !error && (
        <>
          <p className="text-muted small mb-2">{filtered.length} estudiante(s) encontrado(s)</p>
          <div className="card shadow-sm border-0">
            <div className="table-responsive">
              <table className="table table-hover mb-0">
                <thead className="table-light">
                  <tr>
                    <th>Matrícula</th>
                    <th>Nombre</th>
                    <th>Apellido</th>
                    <th>Estado</th>
                    <th className="text-end">Malla</th>
                  </tr>
                </thead>
                <tbody>
                  {filtered.length === 0 && (
                    <tr>
                      <td colSpan={5} className="text-center text-muted py-4">
                        No se encontraron estudiantes
                      </td>
                    </tr>
                  )}
                  {filtered.map((s) => (
                    <tr key={s.id}>
                      <td>
                        <span className="badge bg-secondary font-monospace">
                          {s.enrollmentNumber}
                        </span>
                      </td>
                      <td className="fw-semibold">{s.firstName}</td>
                      <td>{s.lastName}</td>
                      <td>{statusBadge(s.academicStatus)}</td>
                      <td className="text-end">
                        <Link
                          to={`/students/${s.id}/curriculum`}
                          className="btn btn-sm btn-outline-primary"
                        >
                          Ver Malla
                        </Link>
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