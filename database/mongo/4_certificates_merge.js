// database/mongo/4_certificates_merge.js
// Vista materializada "certificado de notas" por estudiante (enunciado, tarea 6).
// Reconstruye certificados_notas mediante un pipeline de agregación con $merge,
// la misma lógica que el backend mantiene reactivamente vía Change Streams sobre grades.
//
// Uso:
//   docker cp database\mongo\4_certificates_merge.js mongo_primary:/tmp/4_certificates_merge.js
//   docker exec -it mongo_primary mongosh "mongodb://localhost:27017/academic_mongo?replicaSet=rs0" /tmp/4_certificates_merge.js

const databaseName = process.env.MONGO_DATABASE || "academic_mongo";
const academicDb = db.getSiblingDB(databaseName);

print(`\n=== Vista materializada certificados_notas (${databaseName}) ===`);

// Reconstrucción limpia: se descarta la vista previa para eliminar documentos
// huérfanos de seeds anteriores.
academicDb.certificados_notas.drop();
academicDb.certificados_notas.createIndex(
  { updatedAt: 1 },
  { name: "idx_certificate_updated_at" }
);
print("certificados_notas: colección reiniciada, índice {updatedAt} creado");

const mergePipeline = [
  // Unir cada calificación con su asignatura y su semestre
  {
    $lookup: {
      from: "subjects",
      localField: "subjectId",
      foreignField: "_id",
      as: "subject"
    }
  },
  { $unwind: "$subject" },
  {
    $lookup: {
      from: "semesters",
      localField: "semesterId",
      foreignField: "_id",
      as: "semester"
    }
  },
  { $unwind: { path: "$semester", preserveNullAndEmptyArrays: true } },
  // Proyección plana por calificación
  {
    $project: {
      _id: { $toString: "$studentId" },
      subjectCode: "$subject.code",
      subjectName: "$subject.name",
      semesterYear: { $ifNull: ["$semester.year", 0] },
      semesterPeriod: { $ifNull: ["$semester.period", ""] },
      grade: "$value",
      recordedAt: "$recordedAt"
    }
  },
  { $sort: { semesterYear: 1, semesterPeriod: 1, subjectCode: 1 } },
  // Agrupar por estudiante construyendo el arreglo entries
  {
    $group: {
      _id: "$_id",
      entries: {
        $push: {
          subjectCode: "$subjectCode",
          subjectName: "$subjectName",
          semesterYear: "$semesterYear",
          semesterPeriod: "$semesterPeriod",
          grade: "$grade",
          recordedAt: "$recordedAt"
        }
      },
      totalRamos: { $sum: 1 },
      sumGrades: { $sum: "$grade" }
    }
  },
  {
    $project: {
      entries: 1,
      totalRamos: 1,
      promedioGeneral: {
        $round: [{ $divide: ["$sumGrades", "$totalRamos"] }, 1]
      },
      updatedAt: new Date()
    }
  },
  // Materialización: reemplaza o inserta el certificado por estudiante
  {
    $merge: {
      into: "certificados_notas",
      on: "_id",
      whenMatched: "replace",
      whenNotMatched: "insert"
    }
  }
];

academicDb.grades.aggregate(mergePipeline).toArray();

print(`certificados_notas materializados: ${academicDb.certificados_notas.countDocuments()}`);

academicDb.certificados_notas.find().forEach((cert) =>
  print(
    `  - ${cert._id}: ${cert.totalRamos} ramo(s), promedio ${cert.promedioGeneral}`
  )
);

print("\n=== Vista materializada reconstruida correctamente ===");
