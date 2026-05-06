import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../../services/api';

export default function SectionList() {
  const [sections, setSections]   = useState([]);
  const [subjects, setSubjects]   = useState([]);
  const [semesters, setSemesters] = useState([]);
  const [loading, setLoading]     = useState(true);
  const [error, setError]         = useState('');

  useEffect(() => {
    const load = async () => {
      try {
        const [secRes, subRes, semRes] = await Promise.all([
          api.get('/api/sections'),
          api.get('/api/subjects'),
          api.get('/api/semesters'),
        ]);
        setSections(secRes.data);
        setSubjects(subRes.data);
        setSemesters(semRes.data);
      } catch {
        setError('Error al cargar las secciones.');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const subjectName  = (id) => subjects.find((s)  => s.id === id)?.name  ?? `#${id}`;
  const semesterLabel = (id) => {
    const s = semesters.find((s) => s.id === id);
    return s ? `${s.year}-${s.period}` : `#${id}`;
  };

  return (
    <div className="container py-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h2 className="fw-bold mb-0">Secciones</h2>
          <p className="text-muted mb-0">Secciones por semestre y asignatura</p>
        </div>
        <Link to="/sections/new" className="btn btn-primary">+ Nueva Sección</Link>
      </div>

      {loading && <p className="text-muted">Cargando...</p>}
      {error   && <div className="alert alert-danger">{error}</div>}

      {!loading && !error && (
        <div className="card shadow-sm border-0">
          <div className="table-responsive">
            <table className="table table-hover mb-0">
              <thead className="table-light">
                <tr>
                  <th>ID</th>
                  <th>Asignatura</th>
                  <th>Semestre</th>
                  <th>Cupos</th>
                  <th>Disponibles</th>
                  <th>Estado</th>
                </tr>
              </thead>
              <tbody>
                {sections.length === 0 && (
                  <tr><td colSpan={6} className="text-center text-muted py-4">No hay secciones registradas</td></tr>
                )}
                {sections.map((s) => (
                  <tr key={s.id}>
                    <td className="text-muted small">{s.id}</td>
                    <td className="fw-semibold">{subjectName(s.subjectId)}</td>
                    <td><span className="badge bg-info text-dark">{semesterLabel(s.semesterId)}</span></td>
                    <td>{s.totalSeats}</td>
                    <td>
                      <span className={s.availableSeats === 0 ? 'text-danger fw-bold' : 'text-success fw-bold'}>
                        {s.availableSeats}
                      </span>
                    </td>
                    <td>
                      <span className={`badge ${s.available ? 'bg-success' : 'bg-secondary'}`}>
                        {s.available ? 'Disponible' : 'Cerrada'}
                      </span>
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