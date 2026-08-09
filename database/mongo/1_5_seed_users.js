const databaseName = process.env.MONGO_DATABASE || "academic_mongo";
const academicDb = db.getSiblingDB(databaseName);

const passwordHash = "$2b$10$TtNwhLLwSL9WQVqfmGAPAe148.KbUbojIaDenf3iwlYABr8UK8Uv.";

const usersData = [
  { id: 1001, rut: "12345678-9", email: "juan@usach.cl", rol: "STUDENT" },
  { id: 1002, rut: "98765432-1", email: "maria@usach.cl", rol: "STUDENT" },
  { id: 1003, rut: "13579246-8", email: "pedro@usach.cl", rol: "STUDENT" },
  { id: 1004, rut: "24681357-9", email: "lucas@usach.cl", rol: "STUDENT" },
  { id: 1005, rut: "12312312-3", email: "valentina@usach.cl", rol: "STUDENT" },
  { id: 1006, rut: "45645645-6", email: "matias@usach.cl", rol: "STUDENT" },
  { id: 2001, rut: "11111111-1", email: "admin@usach.cl", rol: "ADMIN" },
  { id: 2002, rut: "22222222-2", email: "admin2@usach.cl", rol: "ADMIN" },
  { id: 3001, rut: "11222333-4", email: "carlos@usach.cl", rol: "PROFESSOR" },
  { id: 3002, rut: "55666777-8", email: "ana@usach.cl", rol: "PROFESSOR" },
  { id: 3003, rut: "33344455-6", email: "diego@usach.cl", rol: "PROFESSOR" },
  { id: 3004, rut: "44455566-7", email: "elena.vargas@usach.cl", rol: "PROFESSOR" }
];

academicDb.users.deleteMany({});

academicDb.users.insertMany(usersData.map((user) => ({
  id: NumberInt(user.id),
  rut: user.rut,
  email: user.email,
  passwordHash,
  rol: user.rol,
  createdAt: new Date()
})));

print(`users: ${usersData.length} sembrados en ${databaseName}`);
