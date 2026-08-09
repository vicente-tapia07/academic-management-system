import React from "react";
import { Link, useLocation } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";

/**
 * MongoTabs — navegación compartida por las vistas del módulo MongoDB
 * (Laboratorio 3 · Integrante 4 · Frontend 2).
 *
 * Las tres vistas son rutas independientes, así que usa <Link> en lugar del
 * setState que emplea FailureReport.jsx para sus pestañas internas.
 *
 * Cada pestaña declara qué roles pueden verla. Esto refleja en la interfaz las
 * mismas reglas que protegen los endpoints en SecurityConfig: un estudiante
 * solo ve el buscador del catálogo, nunca los reportes ni los certificados.
 */

const TABS = [
  { to: "/mongo/reports",     label: "📊 Aprobación / Reprobación", roles: ["ADMIN", "PROFESSOR"] },
  { to: "/mongo/certificate", label: "📜 Certificado de Notas",     roles: ["ADMIN", "PROFESSOR"] },
  { to: "/mongo/search",      label: "🔎 Buscador de Asignaturas",  roles: ["ADMIN", "PROFESSOR", "STUDENT"] },
];

export default function MongoTabs() {
  const location = useLocation();
  const { isAdmin, isProfessor, isStudent } = useAuth();

  const currentRole = isAdmin ? "ADMIN" : isProfessor ? "PROFESSOR" : isStudent ? "STUDENT" : null;
  const visibleTabs = TABS.filter((tab) => tab.roles.includes(currentRole));

  // Si solo hay una pestaña disponible no vale la pena mostrar la barra.
  if (visibleTabs.length <= 1) return null;

  return (
    <ul className="nav nav-tabs mb-4">
      {visibleTabs.map((tab) => {
        const isActive = location.pathname.startsWith(tab.to);
        return (
          <li className="nav-item" key={tab.to}>
            <Link to={tab.to} className={`nav-link ${isActive ? "active fw-semibold" : ""}`}>
              {tab.label}
            </Link>
          </li>
        );
      })}
    </ul>
  );
}
