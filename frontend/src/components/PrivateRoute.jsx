import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function PrivateRoute({ roles }) {
  const { user, hasRole } = useAuth();

  if (!user) return <Navigate to="/" replace />;

  if (roles && roles.length > 0) {
    const allowed = roles.some((r) => hasRole(r));
    if (!allowed) return <Navigate to="/" replace />;
  }

  return <Outlet />;
}