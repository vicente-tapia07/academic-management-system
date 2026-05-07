import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../services/api';
import { useAuth } from '../../context/AuthContext';

export default function MyGrades() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [grades,  setGrades]  = useState([]);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState('');

  useEffect(() => {
    const load = async () => {
      try {
        const studentsRes = await api.get('/api/students');
        const me = studentsRes.data.find((s) => s.usuarioId === user.id);
        if (!me) {
          setError('No se encontró tu perfil de estudiante.');
          return;
        }
        const gradesRes = await api.get(`/api/grades/student/${me.id}`);
        setGrades(gradesRes.data);
      } catch {
        setError('Error al cargar tus notas.');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [user.id]);

  if (loading) return <p className="text-muted p-4">Cargando notas...</p>;
  if (error)   return <div className="alert alert-danger m-4">{error}</div>;

  return (
    <div className="container py-4">
      <div className="mb-4">
        <button className="btn btn-outline-secondary mb-3" onClick={() => navigate(-1)}>
          ← Volver
        </button>
        <h2 className="fw-bold mb-0">Mis Notas</h2>
        <p className="text-muted mb-0">Historial de calificaciones por asignatura</p>
      </div>

      {grades.length === 0 ? (
        <div className="alert alert-info">No tienes notas registradas aún.</div>
      ) : (
        <div className="card shadow-sm border-0">
          <div className="table-responsive">
            <table className="table table-hover mb-0 align-middle">
              <thead className="table-light">
                <tr>
                  <th>Código</th>
                  <th>Asignatura</th>
                  <th>Nota</th>
                  <th>Fecha</th>
                  <th>Resultado</th>
                </tr>
              </thead>
              <tbody>
                {grades.map((g) => (
                  <tr key={g.gradeId}>
                    <td>
                      <span className="badge bg-primary font-monospace">{g.subjectCode}</span>
                    </td>
                    <td className="fw-semibold">{g.subjectName}</td>
                    <td>
                      <span className={`fw-bold fs-5 ${g.value >= 4.0 ? 'text-success' : 'text-danger'}`}>
                        {Number(g.value).toFixed(1)}
                      </span>
                    </td>
                    <td className="text-muted small">{g.entryDate}</td>
                    <td>
                      <span className={`badge ${g.value >= 4.0 ? 'bg-success' : 'bg-danger'}`}>
                        {g.value >= 4.0 ? 'Aprobado' : 'Reprobado'}
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