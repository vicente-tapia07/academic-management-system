import React from 'react';

export default function AccessibilityBadge({ accessible }) {
  if (accessible === undefined || accessible === null) return null;
  return accessible ? (
    <span className="badge bg-success">♿ Accesible</span>
  ) : (
    <span className="badge bg-secondary">No accesible</span>
  );
}