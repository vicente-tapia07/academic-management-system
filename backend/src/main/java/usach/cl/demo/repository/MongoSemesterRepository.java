package usach.cl.demo.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Repository;
import usach.cl.demo.model.SemesterEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

@Repository
public class MongoSemesterRepository {

    private final MongoCollection<Document> semesters;
    private final MongoCollection<Document> enrollments;
    private final MongoCollection<Document> sections;
    private final MongoCollection<Document> grades;
    private final MongoCollection<Document> subjects;
    private final MongoCollection<Document> students;

    public MongoSemesterRepository(MongoDatabase mongoDatabase) {
        this.semesters = mongoDatabase.getCollection("semesters");
        this.enrollments = mongoDatabase.getCollection("enrollments");
        this.sections = mongoDatabase.getCollection("sections");
        this.grades = mongoDatabase.getCollection("grades");
        this.subjects = mongoDatabase.getCollection("subjects");
        this.students = mongoDatabase.getCollection("students");
    }

    public List<SemesterEntity> findAll() {
        List<SemesterEntity> result = new ArrayList<>();
        for (Document doc : semesters.find()) {
            result.add(mapToSemester(doc));
        }
        return result;
    }

    public SemesterEntity findById(String id) {
        if (!ObjectId.isValid(id)) return null;
        Document doc = semesters.find(eq("_id", new ObjectId(id))).first();
        return doc == null ? null : mapToSemester(doc);
    }

    public SemesterEntity findActive() {
        Document doc = semesters.find(eq("status", "IN_PROGRESS")).first();
        return doc == null ? null : mapToSemester(doc);
    }

    public SemesterEntity save(SemesterEntity semester) {
        validateSemester(semester);
        if (semester.getId() != null && ObjectId.isValid(semester.getId())) {
            semesters.updateOne(eq("_id", new ObjectId(semester.getId())),
                    combine(
                            set("year", semester.getYear()),
                            set("period", semester.getPeriod()),
                            set("startDate", asDate(semester.getStartDate())),
                            set("endDate", asDate(semester.getEndDate())),
                            set("gradeStartDate", asDate(semester.getGradeStartDate())),
                            set("gradeEndDate", asDate(semester.getGradeEndDate())),
                            set("status", semester.getStatus()),
                            set("updatedAt", new Date())));
            return findById(semester.getId());
        }
        Document doc = new Document("_id", new ObjectId())
                .append("year", semester.getYear())
                .append("period", semester.getPeriod())
                .append("startDate", asDate(semester.getStartDate()))
                .append("endDate", asDate(semester.getEndDate()))
                .append("gradeStartDate", asDate(semester.getGradeStartDate()))
                .append("gradeEndDate", asDate(semester.getGradeEndDate()))
                .append("status", semester.getStatus())
                .append("createdAt", new Date());
        semesters.insertOne(doc);
        return findById(doc.getObjectId("_id").toHexString());
    }

    public void deleteById(String id) {
        if (ObjectId.isValid(id)) {
            semesters.deleteOne(eq("_id", new ObjectId(id)));
        }
    }

    /**
     * Replica sp_close_semester: solo se puede cerrar un semestre IN_PROGRESS,
     * sin inscripciones activas sin nota. Al cerrar: los estudiantes con
     * promedio ponderado < 4.0 pasan a BLOCKED y todas las inscripciones
     * activas del semestre pasan a COMPLETED.
     */
    public void closeSemester(String semesterId) {
        if (!ObjectId.isValid(semesterId)) {
            throw new IllegalArgumentException("El semestre no existe");
        }
        SemesterEntity semester = findById(semesterId);
        if (semester == null) {
            throw new IllegalArgumentException("El semestre no existe");
        }
        if (!"IN_PROGRESS".equals(semester.getStatus())) {
            throw new IllegalArgumentException("Solo se puede cerrar un semestre en curso");
        }

        ObjectId semesterObjectId = new ObjectId(semesterId);

        boolean missingGrades = enrollments.find(and(
                eq("semesterId", semesterObjectId),
                eq("status", "ACTIVE")
        )).into(new ArrayList<>()).stream().anyMatch(enrollment -> {
            Object grade = grades.find(eq("enrollmentId", enrollment.getObjectId("_id"))).first();
            return grade == null;
        });
        if (missingGrades) {
            throw new IllegalArgumentException("Existen inscripciones activas sin nota registrada");
        }

        List<Document> semesterEnrollments = enrollments.find(and(
                eq("semesterId", semesterObjectId),
                eq("status", "ACTIVE")
        )).into(new ArrayList<>());

        List<Document> semesterSections = sections.find(eq("semesterId", semesterObjectId)).into(new ArrayList<>());

        List<Document> subjectDocs = subjects.find().into(new ArrayList<>());
        java.util.Map<ObjectId, Integer> creditsBySubject = new java.util.HashMap<>();
        for (Document subject : subjectDocs) {
            creditsBySubject.put(subject.getObjectId("_id"), subject.getInteger("credits", 0));
        }

        java.util.Map<ObjectId, double[]> gradeSumByStudent = new java.util.HashMap<>();
        for (Document enrollment : semesterEnrollments) {
            Document grade = grades.find(eq("enrollmentId", enrollment.getObjectId("_id"))).first();
            if (grade == null) continue;
            ObjectId studentId = enrollment.getObjectId("studentId");
            ObjectId subjectId = enrollment.getObjectId("subjectId");
            Integer credits = creditsBySubject.getOrDefault(subjectId, 0);
            double[] acc = gradeSumByStudent.computeIfAbsent(studentId, k -> new double[]{0.0, 0.0});
            Object rawValue = grade.get("value");
            acc[0] += (rawValue instanceof Number number ? number.doubleValue() : 0.0) * credits;
            acc[1] += credits;
        }

        for (java.util.Map.Entry<ObjectId, double[]> entry : gradeSumByStudent.entrySet()) {
            double[] acc = entry.getValue();
            if (acc[1] <= 0) continue;
            double average = acc[0] / acc[1];
            if (average < 4.0) {
                students.updateOne(eq("_id", entry.getKey()),
                        combine(set("academicStatus", "BLOCKED"), set("updatedAt", new Date())));
            }
        }

        for (Document enrollment : semesterEnrollments) {
            ObjectId enrollmentId = enrollment.getObjectId("_id");
            Document grade = grades.find(eq("enrollmentId", enrollmentId)).first();
            Date completedAt = grade != null ? grade.getDate("recordedAt") : new Date();
            enrollments.updateOne(eq("_id", enrollmentId),
                    combine(set("status", "COMPLETED"),
                            set("completedAt", completedAt),
                            set("updatedAt", new Date())));
        }

        semesters.updateOne(eq("_id", semesterObjectId),
                combine(set("status", "CLOSED"), set("updatedAt", new Date())));
    }

    private void validateSemester(SemesterEntity semester) {
        if (semester.getYear() <= 0) {
            throw new IllegalArgumentException("El año del semestre es obligatorio");
        }
        if (semester.getPeriod() == null || semester.getPeriod().isBlank()) {
            throw new IllegalArgumentException("El período es obligatorio");
        }
        if (semester.getStatus() == null
                || !List.of("PLANNED", "IN_PROGRESS", "CLOSED").contains(semester.getStatus())) {
            throw new IllegalArgumentException("Estado de semestre inválido");
        }
    }

    private static SemesterEntity mapToSemester(Document doc) {
        SemesterEntity semester = new SemesterEntity();
        semester.setId(doc.getObjectId("_id").toHexString());
        semester.setYear(doc.getInteger("year", 0));
        semester.setPeriod(doc.getString("period"));
        semester.setStartDate(asLocalDate(doc.getDate("startDate")));
        semester.setEndDate(asLocalDate(doc.getDate("endDate")));
        semester.setGradeStartDate(asLocalDate(doc.getDate("gradeStartDate")));
        semester.setGradeEndDate(asLocalDate(doc.getDate("gradeEndDate")));
        semester.setStatus(doc.getString("status"));
        return semester;
    }

    private static Date asDate(LocalDate date) {
        if (date == null) return null;
        return Date.from(date.atStartOfDay(ZoneOffset.UTC).toInstant());
    }

    private static LocalDate asLocalDate(Date date) {
        if (date == null) return null;
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneOffset.UTC).toLocalDate();
    }
}
