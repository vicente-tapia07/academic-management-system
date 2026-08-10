// database/mongo/2_indexes.js
// Estrategia de índices del dominio académico (Backend2).
// Se ejecuta DESPUÉS de 1_collections_and_schema.js y ANTES del seed,
// para que los índices únicos impidan duplicados al insertar datos.
//
// Uso (docker-compose lo cablea como servicio mongo-indexes):
//   docker cp database\mongo\2_indexes.js mongo_primary:/tmp/2_indexes.js
//   docker exec -it mongo_primary mongosh "mongodb://localhost:27017/academic_mongo?replicaSet=rs0" /tmp/2_indexes.js

const databaseName = process.env.MONGO_DATABASE || "academic_mongo";
const academicDb = db.getSiblingDB(databaseName);

print(`\n=== Creando índices sobre ${databaseName} ===`);

// -----------------------------------------------------------------------
// users: autenticación JWT y consultas RBAC
// -----------------------------------------------------------------------
// Limpieza de índices legacy que pudieron crearse con nombre por defecto
// (p. ej. "email_1") en versiones anteriores del script de esquema.
["email_1", "id_1", "rol_1"].forEach((legacy) => {
  try {
    academicDb.users.dropIndex(legacy);
    print(`users: índice legacy ${legacy} eliminado`);
  } catch (err) {
    // El índice no existe: no es un problema.
  }
});
academicDb.users.createIndex({ email: 1 }, { name: "uniq_user_email", unique: true });
academicDb.users.createIndex({ id: 1 }, { name: "uniq_user_id", unique: true });
academicDb.users.createIndex({ rol: 1 }, { name: "idx_user_rol" });
print("users: {email} único, {id} único, {rol} creados");

// -----------------------------------------------------------------------
// students: acceso por userId (JWT) y por matrícula
// -----------------------------------------------------------------------
academicDb.students.createIndex({ userId: 1 }, { name: "uniq_student_user", unique: true });
academicDb.students.createIndex(
  { enrollmentNumber: 1 },
  { name: "uniq_student_enrollment", unique: true }
);
print("students: {userId} único, {enrollmentNumber} único creados");

// -----------------------------------------------------------------------
// careers: catálogo por código
// -----------------------------------------------------------------------
academicDb.careers.createIndex({ code: 1 }, { name: "uniq_career_code", unique: true });
print("careers: {code} único creado");

// -----------------------------------------------------------------------
// professors: perfil por userId
// -----------------------------------------------------------------------
academicDb.professors.createIndex({ userId: 1 }, { name: "uniq_professor_user", unique: true });
print("professors: {userId} único creado");

// -----------------------------------------------------------------------
// subjects: catálogo con búsqueda por texto (índice TEXTO)
// -----------------------------------------------------------------------
academicDb.subjects.createIndex({ code: 1 }, { name: "uniq_subject_code", unique: true });
academicDb.subjects.createIndex(
  { name: "text" },
  { name: "text_subject_name", default_language: "spanish" }
);
print("subjects: {code} único, texto sobre {name} creados");

// -----------------------------------------------------------------------
// semesters: semestre activo y unicidad (año, periodo)
// -----------------------------------------------------------------------
academicDb.semesters.createIndex(
  { year: 1, period: 1 },
  { name: "uniq_semester_year_period", unique: true }
);
academicDb.semesters.createIndex(
  { status: 1, year: 1, period: 1 },
  { name: "idx_semester_status" }
);
print("semesters: {year,period} único, {status,year,period} creados");

// -----------------------------------------------------------------------
// sections: secciones disponibles y horarios por profesor
// -----------------------------------------------------------------------
academicDb.sections.createIndex(
  { subjectId: 1, semesterId: 1, status: 1 },
  { name: "idx_section_subject_semester_status" }
);
academicDb.sections.createIndex(
  { professorId: 1, semesterId: 1 },
  { name: "idx_section_professor_semester" }
);
print("sections: {subjectId,semesterId,status}, {professorId,semesterId} creados");

// -----------------------------------------------------------------------
// enrollments: anti-duplicado único + consultas por estudiante/sección
// -----------------------------------------------------------------------
academicDb.enrollments.createIndex(
  { studentId: 1, sectionId: 1, semesterId: 1 },
  { name: "uniq_student_section_semester", unique: true }
);
academicDb.enrollments.createIndex(
  { studentId: 1, status: 1 },
  { name: "idx_student_status" }
);
academicDb.enrollments.createIndex({ sectionId: 1 }, { name: "idx_enrollment_section" });
print("enrollments: {studentId,sectionId,semesterId} único, {studentId,status}, {sectionId} creados");

// -----------------------------------------------------------------------
// grades: reportes por asignatura/semestre, prerrequisitos y nota única
// -----------------------------------------------------------------------
academicDb.grades.createIndex(
  { subjectId: 1, semesterId: 1 },
  { name: "idx_subject_semester" }
);
academicDb.grades.createIndex(
  { studentId: 1, subjectId: 1 },
  { name: "idx_grade_student_subject" }
);
academicDb.grades.createIndex(
  { enrollmentId: 1 },
  { name: "uniq_grade_enrollment", unique: true }
);
print("grades: {subjectId,semesterId}, {studentId,subjectId}, {enrollmentId} único creados");

// -----------------------------------------------------------------------
// audit_logs: expiración TTL de la bitácora (retención 90 días).
// Mongo elimina automáticamente los documentos con operationDate anterior
// a la ventana de retención (requiere Replica Set para el TTL monitor).
// -----------------------------------------------------------------------
const RETENTION_DAYS = 90;
const TTL_SECONDS = RETENTION_DAYS * 24 * 60 * 60;
academicDb.audit_logs.createIndex(
  { operationDate: 1 },
  { name: "ttl_audit_operation_date", expireAfterSeconds: TTL_SECONDS }
);
print(`audit_logs: índice TTL sobre {operationDate} (${RETENTION_DAYS} días) creado`);

// -----------------------------------------------------------------------
// Verificación final
// -----------------------------------------------------------------------
print("\n=== Resumen de índices por colección ===");
academicDb.getCollectionNames()
  .filter((name) => name !== "certificados_notas")
  .sort()
  .forEach((name) => {
    const indexes = academicDb.getCollection(name).getIndexes();
    print(`\n[${name}]`);
    indexes.forEach((idx) =>
      print(
        `  - ${idx.name}: ${EJSON.stringify(idx.key)}${idx.unique ? " (unique)" : ""}${idx.expireAfterSeconds ? ` (TTL ${idx.expireAfterSeconds}s)` : ""}`
      )
    );
  });

print("\n=== Índices creados correctamente ===");
