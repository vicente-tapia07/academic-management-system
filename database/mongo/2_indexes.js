// database/mongo/2_indexes.js
// Backend2 - Índices para el dominio académico
// Se puede ejecutar ANTES o DESPUÉS del seed (no depende de datos, solo de que
// las colecciones existan como resultado de 1_collections_and_schema.js).
//
// Uso:
//   docker cp database\mongo\2_indexes.js mongo_primary:/tmp/2_indexes.js
//   docker exec -it mongo_primary mongosh "mongodb://localhost:27017/academic_mongo?replicaSet=rs0" /tmp/2_indexes.js

const databaseName = process.env.MONGO_DATABASE || "academic_mongo";
const academicDb = db.getSiblingDB(databaseName);

print(`\n=== Creando índices (Backend2) sobre ${databaseName} ===`);

// -----------------------------------------------------------------------
// 1. Índice compuesto ÚNICO en enrollments: {studentId, sectionId, semesterId}
//    Objetivo: impedir que un mismo estudiante quede inscrito dos veces
//    en la misma sección durante el mismo semestre (evita duplicados de
//    inscripción, incluso ante condiciones de carrera si la transacción
//    de Backend1 llegara a fallar a mitad de camino en algún escenario borde).
// -----------------------------------------------------------------------
academicDb.enrollments.createIndex(
  { studentId: 1, sectionId: 1, semesterId: 1 },
  {
    name: "uniq_student_section_semester",
    unique: true
  }
);
print("enrollments: índice único {studentId, sectionId, semesterId} creado");

// Índices de apoyo para las consultas frecuentes del dominio (no exigidos
// explícitamente por el enunciado como "el" índice compuesto, pero son
// justificables para las queries reales de la capa de servicios):

// Buscar todas las inscripciones de un estudiante (GET /enrollments/student/{id})
academicDb.enrollments.createIndex(
  { studentId: 1, status: 1 },
  { name: "idx_student_status" }
);
print("enrollments: índice de apoyo {studentId, status} creado");

// Pipeline de agregación: tasa aprobación/reprobación por asignatura y semestre
academicDb.grades.createIndex(
  { subjectId: 1, semesterId: 1 },
  { name: "idx_subject_semester" }
);
print("grades: índice de apoyo {subjectId, semesterId} creado");

// -----------------------------------------------------------------------
// 2. Índice de TEXTO en subjects.name para el buscador del catálogo
//    (GET /api/mongo/subjects/search?q=...)
// -----------------------------------------------------------------------
academicDb.subjects.createIndex(
  { name: "text" },
  {
    name: "text_subject_name",
    default_language: "spanish"
  }
);
print("subjects: índice de texto {name} creado (idioma: spanish)");

// -----------------------------------------------------------------------
// Verificación final
// -----------------------------------------------------------------------
print("\n=== Índices en enrollments ===");
academicDb.enrollments.getIndexes().forEach((idx) => printjson(idx));

print("\n=== Índices en subjects ===");
academicDb.subjects.getIndexes().forEach((idx) => printjson(idx));

print("\n=== Índices en grades ===");
academicDb.grades.getIndexes().forEach((idx) => printjson(idx));

print("\n=== Índices creados correctamente ===");
