import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';

const statusMap = {
  ACTIVE:    { cls: 'success',   label: 'Activo'     },
  INACTIVE:  { cls: 'secondary', label: 'Inactivo'   },
  SUSPENDED: { cls: 'warning',   label: 'Suspendido' },
  GRADUATED: { cls: 'primary',   label: 'Egresado'   },
};

export default function StudentProfile() {
  const { user }              = useAuth();
  const navigate              = useNavigate();
  const [student, setStudent] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState('');

  useEffect(() => {
    api.get(`/api/students/${user.id}`)
      .then((r) => setStudent(r.data))
      .catch(() => setError('No se pudo cargar el perfil.'))
      .finally(() => setLoading(false));
  }, [user.id]);

  if (loading) return <div className="container py-5 text-muted">Cargando...</div>;
  if (error)   return <div className="container py-5"><div className="alert alert-danger">{error}</div></div>;

  const st = statusMap[student?.academicStatus] ?? { cls: 'secondary', label: student?.academicStatus };

  return (
    <div className="container py-4" style={{ maxWidth: 600 }}>

      <div className="d-flex align-items-center gap-3 mb-4">
        <button className="btn btn-sm btn-outline-secondary" onClick={() => navigate('/my-dashboard')}>
          ← Volver
        </button>
        <h2 className="fw-bold mb-0">Mi Perfil</h2>
      </div>

      <div className="card border-0 shadow-sm">
        <div className="card-body p-4">

          {/* Avatar con iniciales */}
          <div className="text-center mb-4">
            <div
              className="rounded-circle d-inline-flex align-items-center justify-content-center text-white fw-bold"
              style={{ width: 80, height: 80, fontSize: '2rem', backgroundColor: '#003366' }}
            >
              {student?.firstName?.[0]}{student?.lastName?.[0]}
            </div>
            <h4 className="fw-bold mt-3 mb-1">
              {student?.firstName} {student?.lastName}
            </h4>
            <span className={`badge bg-${st.cls}`}>{st.label}</span>
          </div>

          <hr />

          {/* Solo campos que el backend realmente devuelve */}
          {[
            { label: 'Correo',    value: user.email                },
            { label: 'Matrícula', value: student?.enrollmentNumber },
            { label: 'Estado',    value: st.label                  },
          ].map(({ label, value }) => (
            <div key={label} className="d-flex justify-content-between py-2 border-bottom">
              <span className="text-muted small fw-medium">{label}</span>
              <span className="fw-semibold small">{value}</span>
            </div>
          ))}

        </div>
      </div>
    </div>
  );
}