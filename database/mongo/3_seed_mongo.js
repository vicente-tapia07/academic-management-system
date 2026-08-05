// database/mongo/3_seed_mongo.js
// Backend2 - Datos de prueba (seed) para academic_mongo
// Se ejecuta con mongosh DESPUÉS de 1_collections_and_schema.js (colecciones + $jsonSchema ya deben existir)
//
// Uso local (fuera de docker-compose, para pruebas rápidas):
//   mongosh "mongodb://localhost:27017,localhost:27018/?replicaSet=rs0" database/mongo/3_seed_mongo.js
//
// Este script es IDEMPOTENTE: si lo corres varias veces, primero limpia las colecciones
// de dominio (no toca usuarios/roles de Postgres, eso es otra base) y vuelve a insertar.

const databaseName = process.env.MONGO_DATABASE || "academic_mongo";
const academicDb = db.getSiblingDB(databaseName);

print(`\n=== Seed Mongo (Backend2) sobre ${databaseName} ===`);

// -----------------------------------------------------------------------
// 0. Limpieza (idempotencia) - respeta dependencias: grades -> enrollments -> sections
// -----------------------------------------------------------------------
academicDb.grades.deleteMany({});
academicDb.enrollments.deleteMany({});
academicDb.sections.deleteMany({});
academicDb.students.deleteMany({});
academicDb.subjects.deleteMany({});
academicDb.semesters.deleteMany({});
print("Colecciones de dominio limpiadas.");

// -----------------------------------------------------------------------
// 1. Subjects (asignaturas) - una con prerrequisito real de la otra
// -----------------------------------------------------------------------
const subjectIntroId = new ObjectId();
const subjectAvanzadaId = new ObjectId();
const subjectBDId = new ObjectId();

const subjectsDocs = [
  {
    _id: subjectIntroId,
    code: "INF-101",
    name: "Introducción a la Programación",
    credits: NumberInt(6),
    careerCode: "ICINF",
    prerequisiteIds: [],
    active: true,
    createdAt: new Date()
  },
  {
    _id: subjectAvanzadaId,
    code: "INF-201",
    name: "Programación Avanzada",
    credits: NumberInt(6),
    careerCode: "ICINF",
    prerequisiteIds: [subjectIntroId],
    active: true,
    createdAt: new Date()
  },
  {
    _id: subjectBDId,
    code: "INF-310",
    name: "Bases de Datos",
    credits: NumberInt(8),
    careerCode: "ICINF",
    prerequisiteIds: [subjectAvanzadaId],
    active: true,
    createdAt: new Date()
  }
];
academicDb.subjects.insertMany(subjectsDocs);
print(`subjects: ${subjectsDocs.length} insertados`);

// -----------------------------------------------------------------------
// 2. Semesters
// -----------------------------------------------------------------------
const semesterId = new ObjectId();
academicDb.semesters.insertOne({
  _id: semesterId,
  year: NumberInt(2026),
  period: "1S",
  startDate: new Date("2026-03-09T00:00:00Z"),
  endDate: new Date("2026-07-10T00:00:00Z"),
  status: "IN_PROGRESS",
  createdAt: new Date()
});
print("semesters: 1 insertado");

// -----------------------------------------------------------------------
// 3. Sections (una por asignatura, con cupos ya parcialmente ocupados)
// -----------------------------------------------------------------------
const sectionIntroId = new ObjectId();
const sectionAvanzadaId = new ObjectId();
const sectionBDId = new ObjectId();

const sectionsDocs = [
  {
    _id: sectionIntroId,
    subjectId: subjectIntroId,
    semesterId: semesterId,
    professorId: "PROF-001",
    professorName: "María Fernández",
    totalSeats: NumberInt(40),
    availableSeats: NumberInt(37),
    schedule: { dayOfWeek: NumberInt(1), startTime: "08:30", endTime: "10:00" },
    room: { code: "A-101", name: "Sala A-101", building: "Edificio Tecnológico" },
    status: "OPEN",
    createdAt: new Date()
  },
  {
    _id: sectionAvanzadaId,
    subjectId: subjectAvanzadaId,
    semesterId: semesterId,
    professorId: "PROF-002",
    professorName: "Jorge Salinas",
    totalSeats: NumberInt(35),
    availableSeats: NumberInt(33),
    schedule: { dayOfWeek: NumberInt(2), startTime: "10:15", endTime: "11:45" },
    room: { code: "B-204", name: "Sala B-204", building: "Edificio Tecnológico" },
    status: "OPEN",
    createdAt: new Date()
  },
  {
    _id: sectionBDId,
    subjectId: subjectBDId,
    semesterId: semesterId,
    professorId: "PROF-003",
    professorName: "Fernando Solís",
    totalSeats: NumberInt(30),
    availableSeats: NumberInt(1),
    schedule: { dayOfWeek: NumberInt(4), startTime: "14:30", endTime: "16:00" },
    room: { code: "C-305", name: "Sala C-305", building: "Edificio Tecnológico" },
    status: "OPEN",
    createdAt: new Date()
  }
];
academicDb.sections.insertMany(sectionsDocs);
print(`sections: ${sectionsDocs.length} insertadas`);

// -----------------------------------------------------------------------
// 4. Students
// -----------------------------------------------------------------------
const studentsData = [
  { userId: 1001, enrollmentNumber: "202310001", firstName: "Camila", lastName: "Rojas" },
  { userId: 1002, enrollmentNumber: "202310002", firstName: "Benjamín", lastName: "Torres" },
  { userId: 1003, enrollmentNumber: "202310003", firstName: "Valentina", lastName: "Muñoz" },
  { userId: 1004, enrollmentNumber: "202310004", firstName: "Matías", lastName: "Contreras" },
  { userId: 1005, enrollmentNumber: "202310005", firstName: "Antonia", lastName: "Vergara" },
  { userId: 1006, enrollmentNumber: "202310006", firstName: "Diego", lastName: "Silva" }
];

const studentIds = [];
const studentsDocs = studentsData.map((s) => {
  const _id = new ObjectId();
  studentIds.push(_id);
  return {
    _id,
    userId: NumberLong(s.userId),
    enrollmentNumber: s.enrollmentNumber,
    firstName: s.firstName,
    lastName: s.lastName,
    careerCode: "ICINF",
    academicStatus: "ACTIVE",
    createdAt: new Date()
  };
});
academicDb.students.insertMany(studentsDocs);
print(`students: ${studentsDocs.length} insertados`);

// -----------------------------------------------------------------------
// 5. Enrollments + Grades
// -----------------------------------------------------------------------
// businessRules.prerequisitesSatisfied y seatAvailableAtEnrollment DEBEN ser true
// (el $jsonSchema de Backend1 exige enum:[true] en ambos).
//
// Plan:
//  - INF-101 (Intro): los 6 estudiantes inscritos y con la asignatura ya COMPLETED,
//    con notas variadas (aprobados y reprobados) -> alimenta el pipeline pass/fail.
//  - INF-201 (Avanzada): 4 estudiantes ACTIVE (cursando ahora, sin nota aún).
//  - INF-310 (BD): 2 estudiantes ACTIVE (sección casi llena, availableSeats=1).

function makeEnrollment(studentId, sectionId, subjectId, status, enrolledAt, extra) {
  const doc = {
    _id: new ObjectId(),
    studentId,
    sectionId,
    subjectId,
    semesterId: semesterId,
    status,
    businessRules: {
      prerequisitesSatisfied: true,
      seatAvailableAtEnrollment: true,
      validatedAt: enrolledAt
    },
    enrolledAt,
    updatedAt: enrolledAt
  };
  return Object.assign(doc, extra);
}

const enrollmentsDocs = [];
const gradesDocs = [];

// Notas de ejemplo para INF-101 (escala 1.0 - 7.0, aprobación >= 4.0)
const introGrades = [6.5, 5.8, 3.9, 4.0, 2.5, 6.9];

studentIds.forEach((studentId, idx) => {
  const enrolledAt = new Date("2025-08-10T00:00:00Z"); // semestre anterior, ya cerrado
  const completedAt = new Date("2025-12-15T00:00:00Z");

  const enrollment = makeEnrollment(
    studentId,
    sectionIntroId,
    subjectIntroId,
    "COMPLETED",
    enrolledAt,
    { completedAt }
  );
  enrollmentsDocs.push(enrollment);

  gradesDocs.push({
    _id: new ObjectId(),
    enrollmentId: enrollment._id,
    studentId,
    subjectId: subjectIntroId,
    semesterId: semesterId,
    value: introGrades[idx],
    recordedAt: completedAt,
    recordedBy: "PROF-001"
  });
});

// INF-201: 4 de los 6 estudiantes cursando ahora (ACTIVE, sin nota)
const nowEnrolledAt = new Date("2026-03-10T00:00:00Z");
[0, 1, 2, 3].forEach((idx) => {
  enrollmentsDocs.push(
    makeEnrollment(studentIds[idx], sectionAvanzadaId, subjectAvanzadaId, "ACTIVE", nowEnrolledAt)
  );
});

// INF-310: 2 estudiantes cursando ahora (sección casi llena)
[4, 5].forEach((idx) => {
  enrollmentsDocs.push(
    makeEnrollment(studentIds[idx], sectionBDId, subjectBDId, "ACTIVE", nowEnrolledAt)
  );
});

academicDb.enrollments.insertMany(enrollmentsDocs);
print(`enrollments: ${enrollmentsDocs.length} insertadas`);

academicDb.grades.insertMany(gradesDocs);
print(`grades: ${gradesDocs.length} insertadas`);

print("\n=== Seed completado ===");
print(`subjects=${academicDb.subjects.countDocuments()} semesters=${academicDb.semesters.countDocuments()} ` +
      `sections=${academicDb.sections.countDocuments()} students=${academicDb.students.countDocuments()} ` +
      `enrollments=${academicDb.enrollments.countDocuments()} grades=${academicDb.grades.countDocuments()}`);
