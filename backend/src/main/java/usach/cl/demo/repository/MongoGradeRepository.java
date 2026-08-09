package usach.cl.demo.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Repository;
import usach.cl.demo.dto.FailureRateDTO;
import usach.cl.demo.dto.GradeDTO;
import usach.cl.demo.model.GradeEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mongodb.client.model.Filters.eq;

@Repository
public class MongoGradeRepository {

    private final MongoCollection<Document> grades;
    private final MongoCollection<Document> enrollments;
    private final MongoCollection<Document> sections;
    private final MongoCollection<Document> subjects;
    private final MongoCollection<Document> semesters;
    private final MongoCollection<Document> students;

    public MongoGradeRepository(MongoDatabase mongoDatabase) {
        this.grades = mongoDatabase.getCollection("grades");
        this.enrollments = mongoDatabase.getCollection("enrollments");
        this.sections = mongoDatabase.getCollection("sections");
        this.subjects = mongoDatabase.getCollection("subjects");
        this.semesters = mongoDatabase.getCollection("semesters");
        this.students = mongoDatabase.getCollection("students");
    }

    public List<GradeEntity> findAll() {
        List<GradeEntity> result = new ArrayList<>();
        for (Document doc : grades.find()) {
            result.add(mapToGrade(doc));
        }
        return result;
    }

    public GradeEntity save(GradeEntity g, String recordedBy) {
        if (g.getEnrollmentId() == null || !ObjectId.isValid(g.getEnrollmentId())) {
            throw new IllegalArgumentException("Inscripción no encontrada: " + g.getEnrollmentId());
        }
        ObjectId enrollmentId = new ObjectId(g.getEnrollmentId());
        Document enrollment = enrollments.find(eq("_id", enrollmentId)).first();
        if (enrollment == null) {
            throw new IllegalArgumentException("Inscripción no encontrada: " + g.getEnrollmentId());
        }
        String fallbackRecordedBy = null;
        Document section = sections.find(eq("_id", enrollment.getObjectId("sectionId"))).first();
        if (section != null) {
            fallbackRecordedBy = section.getString("professorId");
        }
        String effectiveRecordedBy = (recordedBy == null || recordedBy.isBlank())
                ? fallbackRecordedBy : recordedBy;
        if (effectiveRecordedBy == null) effectiveRecordedBy = "system";

        Date recordedAt = new Date();
        Document existing = grades.find(eq("enrollmentId", enrollmentId)).first();
        if (existing != null) {
            grades.updateOne(eq("_id", existing.getObjectId("_id")), new Document("$set", new Document()
                    .append("value", g.getValue())
                    .append("recordedAt", recordedAt)
                    .append("recordedBy", effectiveRecordedBy)));
            g.setId(existing.getObjectId("_id").toHexString());
            return g;
        }

        Document doc = new Document("_id", new ObjectId())
                .append("studentId", enrollment.getObjectId("studentId"))
                .append("subjectId", enrollment.getObjectId("subjectId"))
                .append("semesterId", enrollment.getObjectId("semesterId"))
                .append("enrollmentId", enrollmentId)
                .append("value", g.getValue())
                .append("recordedAt", recordedAt)
                .append("recordedBy", effectiveRecordedBy);
        grades.insertOne(doc);
        g.setId(doc.getObjectId("_id").toHexString());
        return g;
    }

    public boolean existsByEnrollmentId(String enrollmentId) {
        if (enrollmentId == null || !ObjectId.isValid(enrollmentId)) return false;
        return grades.find(eq("enrollmentId", new ObjectId(enrollmentId))).first() != null;
    }

    public List<GradeDTO> findByStudentId(Long studentId) {
        Document student = students.find(eq("userId", studentId)).first();
        if (student == null) return new ArrayList<>();
        ObjectId studentObjectId = student.getObjectId("_id");

        Map<ObjectId, Document> subjectMap = new HashMap<>();
        for (Document sub : subjects.find()) subjectMap.put(sub.getObjectId("_id"), sub);
        Map<ObjectId, Document> semesterMap = new HashMap<>();
        for (Document sem : semesters.find()) semesterMap.put(sem.getObjectId("_id"), sem);
        Map<ObjectId, String> enrollmentStatus = new HashMap<>();
        for (Document e : enrollments.find(eq("studentId", studentObjectId))) {
            enrollmentStatus.put(e.getObjectId("_id"), e.getString("status"));
        }

        List<GradeDTO> result = new ArrayList<>();
        for (Document doc : grades.find(eq("studentId", studentObjectId))) {
            ObjectId enrollmentId = doc.getObjectId("enrollmentId");
            if (!"COMPLETED".equals(enrollmentStatus.get(enrollmentId))) continue;
            Document subject = subjectMap.get(doc.getObjectId("subjectId"));
            Document semester = semesterMap.get(doc.getObjectId("semesterId"));
            if (subject == null) continue;

            GradeDTO dto = new GradeDTO();
            dto.setGradeId(doc.getObjectId("_id").toHexString());
            dto.setValue(doc.getDouble("value"));
            dto.setEntryDate(toLocalDate(doc.getDate("recordedAt")));
            dto.setSubjectId(subject.getObjectId("_id").toHexString());
            dto.setSubjectCode(subject.getString("code"));
            dto.setSubjectName(subject.getString("name"));
            if (semester != null) {
                dto.setSemesterId(semester.getObjectId("_id").toHexString());
                dto.setSemesterYear(semester.getInteger("year"));
                dto.setSemesterPeriod(semester.getString("period"));
            }
            result.add(dto);
        }

        result.sort(Comparator
                .comparing(GradeDTO::getSemesterYear, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(dto -> dto.getSemesterPeriod() == null ? "" : dto.getSemesterPeriod(),
                        Comparator.reverseOrder())
                .thenComparing(dto -> dto.getSubjectCode() == null ? "" : dto.getSubjectCode()));
        return result;
    }

    public List<FailureRateDTO> getFailureRateReport() {
        List<Document> pipeline = List.of(
                new Document("$match", new Document("value", new Document("$type", "number"))),
                new Document("$bucket",
                        new Document("groupBy", "$value")
                                .append("boundaries", List.of(0, 4.0, 7.0))
                                .append("default", "aprobado")
                                .append("output", new Document("grades", new Document("$push",
                                        new Document("subjectId", "$subjectId")
                                                .append("semesterId", "$semesterId"))))),
                new Document("$unwind", "$grades"),
                new Document("$group",
                        new Document("_id",
                                new Document("subjectId", "$grades.subjectId")
                                        .append("semesterId", "$grades.semesterId"))
                                .append("totalGrades", new Document("$sum", 1))
                                .append("failedGrades", new Document("$sum",
                                        new Document("$cond", List.of(
                                                new Document("$eq", List.of("$_id", 0)),
                                                1,
                                                0))))),
                new Document("$lookup",
                        new Document("from", "subjects")
                                .append("localField", "_id.subjectId")
                                .append("foreignField", "_id")
                                .append("as", "subject")),
                new Document("$unwind", "$subject"),
                new Document("$lookup",
                        new Document("from", "semesters")
                                .append("localField", "_id.semesterId")
                                .append("foreignField", "_id")
                                .append("as", "semester")),
                new Document("$unwind",
                        new Document("path", "$semester").append("preserveNullAndEmptyArrays", true)),
                new Document("$project",
                        new Document("_id", 0)
                                .append("subjectId", new Document("$toString", "$_id.subjectId"))
                                .append("subjectCode", "$subject.code")
                                .append("subjectName", "$subject.name")
                                .append("semesterId", new Document("$toString", "$_id.semesterId"))
                                .append("semesterYear", new Document("$ifNull", List.of("$semester.year", 0)))
                                .append("semesterPeriod", new Document("$ifNull", List.of("$semester.period", "")))
                                .append("totalGrades", 1)
                                .append("failedGrades", 1)
                                .append("failurePercentage", new Document("$round", List.of(
                                        new Document("$divide",
                                                List.of(new Document("$multiply", List.of("$failedGrades", 100)),
                                                        "$totalGrades")),
                                        1)))),
                new Document("$sort",
                        new Document("subjectCode", 1)
                                .append("semesterYear", 1)
                                .append("semesterPeriod", 1))
        );

        List<FailureRateDTO> result = new ArrayList<>();
        for (Document row : grades.aggregate(pipeline)) {
            Number total = row.get("totalGrades", Number.class);
            Number failed = row.get("failedGrades", Number.class);
            FailureRateDTO dto = new FailureRateDTO();
            dto.setSubjectId(row.getString("subjectId"));
            dto.setSubjectCode(row.getString("subjectCode"));
            dto.setSubjectName(row.getString("subjectName"));
            dto.setSemesterId(row.getString("semesterId"));
            dto.setSemesterYear(row.getInteger("semesterYear"));
            dto.setSemesterPeriod(row.getString("semesterPeriod"));
            dto.setTotalGrades(total == null ? 0 : total.intValue());
            dto.setFailedGrades(failed == null ? 0 : failed.intValue());
            dto.setFailurePercentage(row.getDouble("failurePercentage"));
            result.add(dto);
        }
        return result;
    }

    private static GradeEntity mapToGrade(Document doc) {
        GradeEntity g = new GradeEntity();
        g.setId(doc.getObjectId("_id").toHexString());
        g.setEnrollmentId(doc.getObjectId("enrollmentId").toHexString());
        g.setValue(doc.getDouble("value"));
        g.setEntryDate(toLocalDate(doc.getDate("recordedAt")));
        return g;
    }

    private static LocalDate toLocalDate(Date date) {
        if (date == null) return null;
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneOffset.UTC).toLocalDate();
    }
}
