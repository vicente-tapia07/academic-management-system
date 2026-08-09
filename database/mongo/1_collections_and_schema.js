const databaseName = process.env.MONGO_DATABASE || "academic_mongo";
const academicDb = db.getSiblingDB(databaseName);

/*
 * $jsonSchema valida la estructura y las reglas internas de cada documento.
 * No puede consultar otras colecciones. Por eso enrollments exige evidencia
 * de que prerrequisitos y cupo fueron validados, mientras que la comprobación
 * real contra students, grades y sections se realizará dentro de la futura
 * transacción multi-documento.
 */

function applyValidator(collectionName, validator) {
  const exists = academicDb
    .getCollectionInfos({ name: collectionName })
    .some((collection) => collection.name === collectionName);

  const command = exists
    ? {
        collMod: collectionName,
        validator,
        validationLevel: "strict",
        validationAction: "error"
      }
    : {
        create: collectionName,
        validator,
        validationLevel: "strict",
        validationAction: "error"
      };

  const result = academicDb.runCommand(command);
  if (!result.ok) {
    throw new Error(
      `No se pudo ${exists ? "actualizar" : "crear"} ${collectionName}: ${tojson(result)}`
    );
  }

  print(`${collectionName}: ${exists ? "validador actualizado" : "colección creada"}`);
}

const studentsValidator = {
  $jsonSchema: {
    bsonType: "object",
    title: "Estudiante académico",
    description: "Datos de identidad académica sin historial de notas embebido",
    required: [
      "userId",
      "enrollmentNumber",
      "firstName",
      "lastName",
      "careerCode",
      "academicStatus",
      "createdAt"
    ],
    additionalProperties: false,
    properties: {
      _id: { bsonType: "objectId" },
      userId: {
        bsonType: ["int", "long"],
        minimum: 1,
        description: "Identificador del usuario asociado"
      },
      enrollmentNumber: {
        bsonType: "string",
        pattern: "^[0-9]{4,20}$"
      },
      firstName: { bsonType: "string", minLength: 1, maxLength: 100 },
      lastName: { bsonType: "string", minLength: 1, maxLength: 100 },
      careerCode: {
        bsonType: "string",
        pattern: "^[A-Z0-9-]{2,20}$"
      },
      academicStatus: {
        enum: ["ACTIVE", "BLOCKED", "GRADUATED"]
      },
      createdAt: { bsonType: "date" },
      updatedAt: { bsonType: "date" }
    }
  }
};

const subjectsValidator = {
  $jsonSchema: {
    bsonType: "object",
    title: "Asignatura",
    required: ["code", "name", "credits", "careerCode", "prerequisiteIds", "active", "createdAt"],
    additionalProperties: false,
    properties: {
      _id: { bsonType: "objectId" },
      code: {
        bsonType: "string",
        pattern: "^[A-Z0-9-]{2,20}$"
      },
      name: { bsonType: "string", minLength: 2, maxLength: 150 },
      credits: { bsonType: "int", minimum: 1, maximum: 30 },
      careerCode: {
        bsonType: "string",
        pattern: "^[A-Z0-9-]{2,20}$"
      },
      prerequisiteIds: {
        bsonType: "array",
        uniqueItems: true,
        items: { bsonType: "objectId" }
      },
      active: { bsonType: "bool" },
      createdAt: { bsonType: "date" },
      updatedAt: { bsonType: "date" }
    }
  }
};

const semestersValidator = {
  $and: [
    {
      $jsonSchema: {
        bsonType: "object",
        title: "Semestre académico",
        required: ["year", "period", "startDate", "endDate", "status", "createdAt"],
        additionalProperties: false,
        properties: {
          _id: { bsonType: "objectId" },
          year: { bsonType: "int", minimum: 2020, maximum: 2100 },
          period: { enum: ["1S", "2S", "SUMMER"] },
          startDate: { bsonType: "date" },
          endDate: { bsonType: "date" },
          status: { enum: ["PLANNED", "IN_PROGRESS", "CLOSED"] },
          createdAt: { bsonType: "date" },
          updatedAt: { bsonType: "date" }
        }
      }
    },
    {
      $expr: { $lt: ["$startDate", "$endDate"] }
    }
  ]
};

const sectionsValidator = {
  $and: [
    {
      $jsonSchema: {
        bsonType: "object",
        title: "Sección académica",
        required: [
          "subjectId",
          "semesterId",
          "professorId",
          "professorName",
          "totalSeats",
          "availableSeats",
          "schedule",
          "room",
          "status",
          "createdAt"
        ],
        additionalProperties: false,
        properties: {
          _id: { bsonType: "objectId" },
          subjectId: { bsonType: "objectId" },
          semesterId: { bsonType: "objectId" },
          professorId: { bsonType: "string", minLength: 1, maxLength: 30 },
          professorName: { bsonType: "string", minLength: 2, maxLength: 200 },
          totalSeats: { bsonType: "int", minimum: 1, maximum: 500 },
          availableSeats: { bsonType: "int", minimum: 0, maximum: 500 },
          schedule: {
            bsonType: "object",
            required: ["dayOfWeek", "startTime", "endTime"],
            additionalProperties: false,
            properties: {
              dayOfWeek: { bsonType: "int", minimum: 1, maximum: 6 },
              startTime: {
                bsonType: "string",
                pattern: "^([01][0-9]|2[0-3]):[0-5][0-9]$"
              },
              endTime: {
                bsonType: "string",
                pattern: "^([01][0-9]|2[0-3]):[0-5][0-9]$"
              }
            }
          },
          room: {
            bsonType: "object",
            required: ["code", "name", "building"],
            additionalProperties: false,
            properties: {
              code: { bsonType: "string", minLength: 1, maxLength: 30 },
              name: { bsonType: "string", minLength: 1, maxLength: 100 },
              building: { bsonType: "string", minLength: 1, maxLength: 100 }
            }
          },
          status: { enum: ["OPEN", "CLOSED", "CANCELLED"] },
          createdAt: { bsonType: "date" },
          updatedAt: { bsonType: "date" }
        }
      }
    },
    {
      $expr: {
        $and: [
          { $lte: ["$availableSeats", "$totalSeats"] },
          { $lt: ["$schedule.startTime", "$schedule.endTime"] }
        ]
      }
    }
  ]
};

const enrollmentsValidator = {
  $and: [
    {
      $jsonSchema: {
        bsonType: "object",
        title: "Inscripción académica",
        description: "Referencia estudiante, sección, asignatura y semestre; la nota no se embebe",
        required: [
          "studentId",
          "sectionId",
          "subjectId",
          "semesterId",
          "status",
          "businessRules",
          "enrolledAt",
          "updatedAt"
        ],
        additionalProperties: false,
        properties: {
          _id: { bsonType: "objectId" },
          studentId: { bsonType: "objectId" },
          sectionId: { bsonType: "objectId" },
          subjectId: { bsonType: "objectId" },
          semesterId: { bsonType: "objectId" },
          status: { enum: ["ACTIVE", "CANCELLED", "COMPLETED"] },
          businessRules: {
            bsonType: "object",
            required: ["prerequisitesSatisfied", "seatAvailableAtEnrollment", "validatedAt"],
            additionalProperties: false,
            properties: {
              prerequisitesSatisfied: {
                enum: [true],
                description: "La transacción comprobó todos los prerrequisitos"
              },
              seatAvailableAtEnrollment: {
                enum: [true],
                description: "La transacción reservó un cupo válido"
              },
              validatedAt: { bsonType: "date" }
            }
          },
          enrolledAt: { bsonType: "date" },
          cancelledAt: { bsonType: "date" },
          completedAt: { bsonType: "date" },
          updatedAt: { bsonType: "date" }
        }
      }
    },
    {
      $or: [
        { status: { $ne: "CANCELLED" } },
        { cancelledAt: { $type: "date" } }
      ]
    },
    {
      $or: [
        { status: { $ne: "COMPLETED" } },
        { completedAt: { $type: "date" } }
      ]
    }
  ]
};

const gradesValidator = {
  $jsonSchema: {
    bsonType: "object",
    title: "Calificación final referenciada",
    description: "La nota referencia una inscripción y no se embebe en students",
    required: [
      "enrollmentId",
      "studentId",
      "subjectId",
      "semesterId",
      "value",
      "recordedAt",
      "recordedBy"
    ],
    additionalProperties: false,
    properties: {
      _id: { bsonType: "objectId" },
      enrollmentId: { bsonType: "objectId" },
      studentId: { bsonType: "objectId" },
      subjectId: { bsonType: "objectId" },
      semesterId: { bsonType: "objectId" },
      value: {
        bsonType: ["double", "int", "long", "decimal"],
        minimum: 1.0,
        maximum: 7.0
      },
      recordedAt: { bsonType: "date" },
      recordedBy: { bsonType: "string", minLength: 1, maxLength: 100 },
      updatedAt: { bsonType: "date" }
    }
  }
};

const usersValidator = {
  $jsonSchema: {
    bsonType: "object",
    title: "Usuario del sistema",
    description: "Credenciales de autenticación gestionadas en MongoDB",
    required: ["id", "rut", "email", "passwordHash", "rol", "createdAt"],
    additionalProperties: false,
    properties: {
      _id: { bsonType: "objectId" },
      id: {
        bsonType: ["int", "long"],
        minimum: 1,
        description: "Identificador único del usuario"
      },
      rut: {
        bsonType: "string",
        pattern: "^[0-9]{7,8}-[0-9Kk]{1}$"
      },
      email: {
        bsonType: "string",
        pattern: "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"
      },
      passwordHash: { bsonType: "string", minLength: 20, maxLength: 100 },
      rol: { enum: ["STUDENT", "PROFESSOR", "ADMIN"] },
      createdAt: { bsonType: "date" },
      updatedAt: { bsonType: "date" }
    }
  }
};

applyValidator("students", studentsValidator);
applyValidator("subjects", subjectsValidator);
applyValidator("semesters", semestersValidator);
applyValidator("sections", sectionsValidator);
applyValidator("enrollments", enrollmentsValidator);
applyValidator("grades", gradesValidator);
applyValidator("users", usersValidator);

academicDb.users.createIndex({ email: 1 }, { unique: true });

print(`Esquema MongoDB aplicado correctamente en ${databaseName}`);
