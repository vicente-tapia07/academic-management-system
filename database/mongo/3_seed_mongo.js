// database/mongo/3_seed_mongo.js
// Seed de dominio para academic_mongo: port del fixture coherente de
// 2_db_mock.sql (Laboratorio 1/2) al modelo documental.
//
// Se ejecuta con mongosh DESPUÉS de 1_collections_and_schema.js e
// 2_indexes.js (docker-compose lo cablea como servicio mongo-seed).
//
// IDEMPOTENTE: limpia las colecciones de dominio (no toca `users`, que
// pertenece a 1_5_seed_users.js) y vuelve a insertar.

const databaseName = process.env.MONGO_DATABASE || "academic_mongo";
const academicDb = db.getSiblingDB(databaseName);

print(`\n=== Seed Mongo (Backend2) sobre ${databaseName} ===`);

// -----------------------------------------------------------------------
// 0. Limpieza (idempotencia) - respeta dependencias
// -----------------------------------------------------------------------
academicDb.audit_logs.deleteMany({});
academicDb.grades.deleteMany({});
academicDb.enrollments.deleteMany({});
academicDb.sections.deleteMany({});
academicDb.students.deleteMany({});
academicDb.subjects.deleteMany({});
academicDb.semesters.deleteMany({});
academicDb.professors.deleteMany({});
academicDb.careers.deleteMany({});
print("Colecciones de dominio limpiadas.");

const now = () => new Date();

// -----------------------------------------------------------------------
// 1. Careers (carreras)
// -----------------------------------------------------------------------
const careersData = [
  { code: "INF", name: "Ingeniería Informática" },
  { code: "ICI", name: "Ingeniería Civil Industrial" }
];
const careerDocs = {};
careersData.forEach((c) => {
  const doc = { _id: new ObjectId(), code: c.code, name: c.name, createdAt: now() };
  careerDocs[c.code] = doc;
});
academicDb.careers.insertMany(Object.values(careerDocs));
print(`careers: ${careersData.length} insertadas`);

// -----------------------------------------------------------------------
// 2. Professors (perfil académico referenciado por sections.professorId)
//    professorId en sections = users.id (cadena) del profesor.
// -----------------------------------------------------------------------
const professorsData = [
  { userId: 3001, firstName: "Carlos", lastName: "Ruiz", department: "Informática" },
  { userId: 3002, firstName: "Ana", lastName: "López", department: "Matemáticas" },
  { userId: 3003, firstName: "Diego", lastName: "Mora", department: "Matemáticas" },
  { userId: 3004, firstName: "Elena", lastName: "Vargas", department: "Física" }
];
const professorDocs = {};
professorsData.forEach((p) => {
  const doc = {
    _id: new ObjectId(),
    userId: NumberInt(p.userId),
    firstName: p.firstName,
    lastName: p.lastName,
    department: p.department,
    createdAt: now()
  };
  professorDocs[p.userId] = doc;
});
academicDb.professors.insertMany(Object.values(professorDocs));
print(`professors: ${professorsData.length} insertados`);

// -----------------------------------------------------------------------
// 3. Subjects (asignaturas con prerrequisitos por referencia)
// -----------------------------------------------------------------------
const subjectSpecs = [
  { code: "CAL1", name: "Cálculo 1", credits: 4, prereqCodes: [] },
  { code: "CAL2", name: "Cálculo 2", credits: 4, prereqCodes: ["CAL1"] },
  { code: "PRG1", name: "Programación 1", credits: 4, prereqCodes: [] },
  { code: "PRG2", name: "Programación 2", credits: 4, prereqCodes: ["PRG1"] },
  { code: "BDD1", name: "Base de Datos", credits: 4, prereqCodes: ["PRG1"] },
  { code: "ALG1", name: "Álgebra 1", credits: 3, prereqCodes: [] },
  { code: "FIS1", name: "Física 1", credits: 4, prereqCodes: [] },
  { code: "ALG2", name: "Álgebra 2", credits: 3, prereqCodes: ["ALG1"] },
  { code: "FIS2", name: "Física 2", credits: 4, prereqCodes: ["FIS1"] }
];
const subjectDocs = {};
subjectSpecs.forEach((s) => {
  subjectDocs[s.code] = {
    _id: new ObjectId(),
    code: s.code,
    name: s.name,
    credits: NumberInt(s.credits),
    careerCode: "INF",
    active: true,
    createdAt: now()
  };
});
subjectSpecs.forEach((s) => {
  subjectDocs[s.code].prerequisiteIds = s.prereqCodes.map((c) => subjectDocs[c]._id);
});
academicDb.subjects.insertMany(Object.values(subjectDocs));
print(`subjects: ${subjectSpecs.length} insertadas`);

// -----------------------------------------------------------------------
// 4. Semesters (con ventana de notas para la regla de calendario)
// -----------------------------------------------------------------------
const semesterSpecs = [
  { year: 2024, period: "1S", start: "2024-03-04", end: "2024-07-26", gStart: "2024-06-17", gEnd: "2024-07-26", status: "CLOSED" },
  { year: 2024, period: "2S", start: "2024-08-05", end: "2024-12-20", gStart: "2024-11-25", gEnd: "2024-12-20", status: "CLOSED" },
  { year: 2025, period: "1S", start: "2025-03-03", end: "2025-07-25", gStart: "2025-06-16", gEnd: "2025-07-25", status: "CLOSED" },
  { year: 2025, period: "2S", start: "2025-08-04", end: "2025-12-19", gStart: "2025-11-24", gEnd: "2025-12-19", status: "CLOSED" },
  { year: 2026, period: "1S", start: "2026-03-02", end: "2026-08-14", gStart: "2026-07-13", gEnd: "2026-08-14", status: "IN_PROGRESS" },
  { year: 2026, period: "2S", start: "2026-08-17", end: "2026-12-18", gStart: "2026-11-23", gEnd: "2026-12-18", status: "PLANNED" }
];
const semesterDocs = {};
semesterSpecs.forEach((sem) => {
  const doc = {
    _id: new ObjectId(),
    year: NumberInt(sem.year),
    period: sem.period,
    startDate: new Date(sem.start + "T00:00:00Z"),
    endDate: new Date(sem.end + "T00:00:00Z"),
    gradeStartDate: new Date(sem.gStart + "T00:00:00Z"),
    gradeEndDate: new Date(sem.gEnd + "T00:00:00Z"),
    status: sem.status,
    createdAt: now()
  };
  semesterDocs[`${sem.year}-${sem.period}`] = doc;
});
academicDb.semesters.insertMany(Object.values(semesterDocs));
print(`semesters: ${semesterSpecs.length} insertados`);

// -----------------------------------------------------------------------
// 5. Students
// -----------------------------------------------------------------------
const studentsData = [
  { userId: 1001, enrollmentNumber: "2024001", firstName: "Juan", lastName: "Pérez" },
  { userId: 1002, enrollmentNumber: "2024002", firstName: "María", lastName: "González" },
  { userId: 1003, enrollmentNumber: "2024003", firstName: "Pedro", lastName: "Soto" },
  { userId: 1004, enrollmentNumber: "2024004", firstName: "Lucas", lastName: "Torres" },
  { userId: 1005, enrollmentNumber: "2024005", firstName: "Valentina", lastName: "Muñoz" },
  { userId: 1006, enrollmentNumber: "2024006", firstName: "Matías", lastName: "Contreras" }
];
const studentDocs = {
  byEnrollment: {},
  byUserId: {}
};
studentsData.forEach((s) => {
  const doc = {
    _id: new ObjectId(),
    userId: NumberLong(String(s.userId)),
    enrollmentNumber: s.enrollmentNumber,
    firstName: s.firstName,
    lastName: s.lastName,
    careerCode: "INF",
    academicStatus: "ACTIVE",
    createdAt: now()
  };
  studentDocs.byEnrollment[s.enrollmentNumber] = doc;
  studentDocs.byUserId[s.userId] = doc;
});
academicDb.students.insertMany(Object.values(studentDocs.byEnrollment));
print(`students: ${studentsData.length} insertados`);

// -----------------------------------------------------------------------
// 6. Sections (sala embebida, profesor por referencia userId)
// -----------------------------------------------------------------------
const ROOMS = {
  "A-101": { code: "A-101", name: "Sala 101", building: "Facultad de Ingeniería" },
  "A-102": { code: "A-102", name: "Sala 102", building: "Facultad de Ingeniería" },
  "B-201": { code: "B-201", name: "Sala 201", building: "Facultad de Ciencias" },
  "B-202": { code: "B-202", name: "Sala 202", building: "Facultad de Ciencias" }
};

// [subjectCode, profesorUserId, year, period, roomCode, dayOfWeek, start, end, availableSeats]
const sectionSpecs = [
  // 2024-1S
  ["CAL1", 3002, 2024, "1S", "A-101", 1, "08:15", "09:35", 16],
  ["ALG1", 3003, 2024, "1S", "A-102", 2, "08:15", "09:35", 16],
  ["FIS1", 3004, 2024, "1S", "B-201", 1, "09:50", "11:10", 17],
  // 2024-2S
  ["CAL1", 3002, 2024, "2S", "A-101", 1, "08:15", "09:35", 19],
  ["ALG1", 3003, 2024, "2S", "A-102", 2, "08:15", "09:35", 18],
  ["FIS1", 3004, 2024, "2S", "B-201", 1, "09:50", "11:10", 18],
  ["CAL2", 3002, 2024, "2S", "A-101", 3, "09:50", "11:10", 19],
  ["ALG2", 3003, 2024, "2S", "A-102", 4, "09:50", "11:10", 19],
  ["FIS2", 3004, 2024, "2S", "B-201", 3, "08:15", "09:35", 19],
  ["PRG1", 3001, 2024, "2S", "A-101", 2, "09:50", "11:10", 18],
  // 2025-1S
  ["CAL2", 3002, 2025, "1S", "A-101", 3, "09:50", "11:10", 18],
  ["ALG2", 3003, 2025, "1S", "A-102", 4, "09:50", "11:10", 19],
  ["FIS2", 3004, 2025, "1S", "B-201", 3, "08:15", "09:35", 17],
  ["PRG2", 3001, 2025, "1S", "A-102", 3, "13:45", "15:05", 19],
  ["BDD1", 3001, 2025, "1S", "B-202", 5, "08:15", "09:35", 19],
  // 2025-2S
  ["CAL2", 3002, 2025, "2S", "A-101", 3, "09:50", "11:10", 18],
  ["ALG2", 3003, 2025, "2S", "A-102", 4, "09:50", "11:10", 19],
  ["PRG2", 3001, 2025, "2S", "A-102", 3, "13:45", "15:05", 19],
  // 2026-1S (sección activa por profesor)
  ["CAL1", 3002, 2026, "1S", "A-101", 1, "08:15", "09:35", 20],
  ["PRG1", 3001, 2026, "1S", "A-101", 2, "09:50", "11:10", 19],
  ["FIS2", 3004, 2026, "1S", "B-201", 3, "08:15", "09:35", 19],
  ["ALG2", 3003, 2026, "1S", "A-102", 4, "09:50", "11:10", 19],
  ["PRG2", 3001, 2026, "1S", "A-102", 3, "13:45", "15:05", 19]
];

const sectionDocs = {};
sectionSpecs.forEach(([subjectCode, professorUserId, year, period, roomCode, dayOfWeek, start, end, availableSeats]) => {
  const professor = professorDocs[professorUserId];
  const doc = {
    _id: new ObjectId(),
    subjectId: subjectDocs[subjectCode]._id,
    semesterId: semesterDocs[`${year}-${period}`]._id,
    professorId: String(professorUserId),
    professorName: `${professor.firstName} ${professor.lastName}`,
    totalSeats: NumberInt(20),
    availableSeats: NumberInt(availableSeats),
    schedule: { dayOfWeek: NumberInt(dayOfWeek), startTime: start, endTime: end },
    room: ROOMS[roomCode],
    status: semesterDocs[`${year}-${period}`].status === "IN_PROGRESS" ? "OPEN" : "CLOSED",
    createdAt: now()
  };
  sectionDocs[`${subjectCode}|${year}-${period}`] = doc;
});
academicDb.sections.insertMany(Object.values(sectionDocs));
print(`sections: ${sectionSpecs.length} insertadas`);

// -----------------------------------------------------------------------
// 7. Enrollments + Grades (historial cerrado + activas 2026-1S)
//    Formato por semestre: [enrollmentNumber, subjectCode, grade|null]
// -----------------------------------------------------------------------
const history = [
  {
    year: 2024, period: "1S", enrolledAt: "2024-03-04", gradeDate: "2024-07-01",
    rows: [
      ["2024001", "CAL1", 5.6], ["2024001", "ALG1", 5.2],
      ["2024002", "CAL1", 3.2], ["2024002", "ALG1", 3.6], ["2024002", "FIS1", 4.6],
      ["2024003", "CAL1", 6.2], ["2024003", "ALG1", 5.9], ["2024003", "FIS1", 6.0],
      ["2024004", "CAL1", 4.1], ["2024004", "ALG1", 3.8], ["2024004", "FIS1", 3.5]
    ]
  },
  {
    year: 2024, period: "2S", enrolledAt: "2024-08-05", gradeDate: "2024-12-02",
    rows: [
      ["2024001", "FIS1", 5.0],
      ["2024002", "CAL1", 4.3], ["2024002", "ALG1", 4.1],
      ["2024003", "CAL2", 5.8], ["2024003", "ALG2", 5.5], ["2024003", "FIS2", 5.6], ["2024003", "PRG1", 6.3],
      ["2024004", "ALG1", 4.0], ["2024004", "FIS1", 4.2], ["2024004", "PRG1", 5.0]
    ]
  },
  {
    year: 2025, period: "1S", enrolledAt: "2025-03-03", gradeDate: "2025-07-01",
    rows: [
      ["2024001", "FIS2", 5.1], ["2024001", "CAL2", 4.8],
      ["2024002", "FIS2", 3.7],
      ["2024003", "PRG2", 6.0], ["2024003", "BDD1", 5.7],
      ["2024004", "CAL2", 3.9], ["2024004", "ALG2", 4.4], ["2024004", "FIS2", 4.0]
    ]
  },
  {
    year: 2025, period: "2S", enrolledAt: "2025-08-04", gradeDate: "2025-12-01",
    rows: [
      ["2024001", "ALG2", 5.4],
      ["2024002", "CAL2", 4.2],
      ["2024004", "CAL2", 4.2], ["2024004", "PRG2", 3.6]
    ]
  },
  {
    year: 2026, period: "1S", enrolledAt: "2026-03-02", gradeDate: null,
    rows: [
      ["2024001", "PRG1", null],
      ["2024002", "FIS2", null], ["2024002", "ALG2", null],
      ["2024004", "PRG2", null]
    ]
  }
];

const enrollmentsDocs = [];
const gradesDocs = [];

history.forEach((semester) => {
  const semesterKey = `${semester.year}-${semester.period}`;
  semester.rows.forEach(([enrollmentNumber, subjectCode, grade]) => {
    const student = studentDocs.byEnrollment[enrollmentNumber];
    const section = sectionDocs[`${subjectCode}|${semesterKey}`];
    if (!student || !section) {
      throw new Error(`Seed inconsistente: ${enrollmentNumber}/${subjectCode}/${semesterKey}`);
    }

    const enrolledAt = new Date(semester.enrolledAt + "T00:00:00Z");
    const isCompleted = grade !== null;
    const completedAt = isCompleted
      ? new Date(semester.gradeDate + "T00:00:00Z")
      : undefined;

    const enrollmentDoc = {
      _id: new ObjectId(),
      studentId: student._id,
      sectionId: section._id,
      subjectId: section.subjectId,
      semesterId: section.semesterId,
      status: isCompleted ? "COMPLETED" : "ACTIVE",
      businessRules: {
        prerequisitesSatisfied: true,
        seatAvailableAtEnrollment: true,
        validatedAt: enrolledAt
      },
      enrolledAt,
      updatedAt: enrolledAt
    };
    if (isCompleted) {
      enrollmentDoc.completedAt = completedAt;
    }
    enrollmentsDocs.push(enrollmentDoc);

    if (isCompleted) {
      gradesDocs.push({
        _id: new ObjectId(),
        enrollmentId: enrollmentDoc._id,
        studentId: student._id,
        subjectId: section.subjectId,
        semesterId: section.semesterId,
        value: grade,
        recordedAt: completedAt,
        recordedBy: section.professorId
      });
    }
  });
});

academicDb.enrollments.insertMany(enrollmentsDocs);
print(`enrollments: ${enrollmentsDocs.length} insertadas`);

academicDb.grades.insertMany(gradesDocs);
print(`grades: ${gradesDocs.length} insertadas`);

// -----------------------------------------------------------------------
// Verificación final
// -----------------------------------------------------------------------
print("\n=== Seed completado ===");
print(
  `careers=${academicDb.careers.countDocuments()} professors=${academicDb.professors.countDocuments()} ` +
  `subjects=${academicDb.subjects.countDocuments()} semesters=${academicDb.semesters.countDocuments()} ` +
  `sections=${academicDb.sections.countDocuments()} students=${academicDb.students.countDocuments()} ` +
  `enrollments=${academicDb.enrollments.countDocuments()} grades=${academicDb.grades.countDocuments()}`
);
