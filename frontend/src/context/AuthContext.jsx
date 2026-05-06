import React, { createContext, useContext, useState, useCallback } from 'react';

const AuthContext = createContext(null);

function decodeToken(token) {
  try {
    const payload = token.split('.')[1];
    return JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
  } catch {
    return null;
  }
}

function getStoredUser() {
  try {
    const token = localStorage.getItem('token');
    if (!token) return null;
    const decoded = decodeToken(token);
    if (!decoded) return null;
    if (decoded.exp && decoded.exp * 1000 < Date.now()) {
      localStorage.removeItem('token');
      return null;
    }
    return { id: decoded.id, email: decoded.sub, roles: decoded.roles || [] };
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(getStoredUser);

  const loginUser = useCallback((token) => {
    localStorage.setItem('token', token);
    const decoded = decodeToken(token);
    if (!decoded) return null;
    const userData = { id: decoded.id, email: decoded.sub, roles: decoded.roles || [] };
    setUser(userData);
    return userData;
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('token');
    setUser(null);
  }, []);

  const hasRole = useCallback(
    (role) =>
      user?.roles?.includes(role) || user?.roles?.includes(`ROLE_${role}`),
    [user]
  );

  const isAdmin     = hasRole('ADMIN');
  const isProfessor = hasRole('PROFESSOR');
  const isStudent   = hasRole('STUDENT');

  return (
    <AuthContext.Provider value={{ user, loginUser, logout, hasRole, isAdmin, isProfessor, isStudent }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth debe usarse dentro de AuthProvider');
  return ctx;
}