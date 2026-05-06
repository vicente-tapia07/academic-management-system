import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../../services/api';

const statusBadge = (s) => {
  if (s.closed) return <span className="badge bg-dark">Cerrado</span>;
  if (s.active)  return <span className="badge bg-success">Activo</span>;
  return <span className="badge bg-secondary">Inactivo</span>;
};

export default function SemesterList() {
  const [semesters, setSemesters] = useState([]);
  const [loading, setLoading]     = useState(true);
  const [error, setError]         = useState('');

  const load = async () => {
    try {
      setLoading(true);
      const res = await api.get('/api/semesters');
      setSemesters(res.data);
    } catch {
      setError('Error al cargar los semestres.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  return (
    <div className="container py-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h2 className="fw-bold mb-0">Semestres</h2>
          <p className="text-muted mb-0">Gestiona los períodos académicos</p>
        </div>
        <Link to="/semesters/new" className="btn btn-primary">+ Nuevo Semestre</Link>
      </div>

      {loading && <p className="text-muted">Cargando...</p>}
      {error   && <div className="alert alert-danger">{error}</div>}

      {!loading && !error && (
        <div className="card shadow-sm border-0">
          <div className="table-responsive">
            <table className="table table-hover mb-0">
              <thead className="table-light">
                <tr>
                  <th>Año</th>
                  <th>Período</th>
                  <th>Inicio</th>
                  <th>Fin</th>
                  <th>Período Notas</th>
                  <th>Estado</th>
                  <th className="text-end">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {semesters.length === 0 && (
                  <tr><td colSpan={7} className="text-center text-muted py-4">No hay semestres registrados</td></tr>
                )}
                {semesters.map((s) => (
                  <tr key={s.id}>
                    <td className="fw-semibold">{s.year}</td>
                    <td>{s.period}</td>
                    <td className="text-muted small">{s.startDate}</td>
                    <td className="text-muted small">{s.endDate}</td>
                    <td className="text-muted small">
                      {s.gradeStartDate} → {s.gradeEndDate}
                    </td>
                    <td>{statusBadge(s)}</td>
                    <td className="text-end">
                      {!s.closed && (
                        <>
                          <Link to={`/semesters/edit/${s.id}`} className="btn btn-sm btn-outline-primary me-2">
                            Editar
                          </Link>
                          <Link to={`/semesters/close/${s.id}`} className="btn btn-sm btn-outline-danger">
                            Cerrar
                          </Link>
                        </>
                      )}
                      {s.closed && <span className="text-muted small">—</span>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}