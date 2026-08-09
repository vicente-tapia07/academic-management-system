package usach.cl.demo.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Repository;
import usach.cl.demo.dto.StudentDTO;
import usach.cl.demo.dto.SubjectStatusDTO;
import usach.cl.demo.model.Role;
import usach.cl.demo.model.StudentEntity;
import usach.cl.demo.model.UserEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

@Repository
public class MongoStudentRepository {

    private final MongoCollection<Document> students;
    private final MongoCollection<Document> enrollments;
    private final MongoCollection<Document> sections;
    private final MongoCollection<Document> subjects;
    private final MongoCollection<Document> semesters;
    private final MongoCollection<Document> grades;
    private final MongoUserRepository userRepository;

    public MongoStudentRepository(MongoDatabase mongoDatabase, MongoUserRepository userRepository) {
        this.students = mongoDatabase.getCollection("students");
        this.enrollments = mongoDatabase.getCollection("enrollments");
        this.sections = mongoDatabase.getCollection("sections");
        this.subjects = mongoDatabase.getCollection("subjects");
        this.semesters = mongoDatabase.getCollection("semesters");
        this.grades = mongoDatabase.getCollection("grades");
        this.userRepository = userRepository;
    }

    public List<StudentEntity> findAll() {
        List<StudentEntity> result = new ArrayList<>();
        for (Document doc : students.find()) {
            result.add(mapToStudent(doc));
        }
        return result;
    }

    public StudentEntity findById(Long id) {
        if (id == null) return null;
        Document doc = students.find(eq("userId", id)).first();
        return doc == null ? null : mapToStudent(doc);
    }

    public StudentEntity saveWithUsuario(StudentDTO dto, String passwordHash) {
        if (dto.email() == null || dto.email().isBlank()) {
            throw new IllegalArgumentException("El email es obligatorio");
        }
        if (userRepository.existsByEmail(dto.email())) {
            throw new IllegalArgumentException("El email ya está en uso");
        }
        String rut = validRut(dto.rut());
        UserEntity user = userRepository.save(
                new UserEntity(-1, rut, dto.email(), passwordHash, Role.STUDENT));

        Document doc = new Document("_id", new ObjectId())
                .append("userId", user.getId())
                .append("enrollmentNumber", dto.enrollmentNumber())
                .append("firstName", dto.firstName())
                .append("lastName", dto.lastName())
                .append("careerCode", "INF")
                .append("academicStatus", "ACTIVE")
                .append("createdAt", new Date());
        students.insertOne(doc);
        return mapToStudent(doc);
    }

    public void update(StudentEntity student) {
        students.updateOne(eq("userId", student.getId()),
                combine(set("firstName", student.getFirstName()),
                        set("lastName", student.getLastName()),
                        set("academicStatus", student.getAcademicStatus()),
                        set("updatedAt", new Date())));
    }

    public void deleteById(Long userId) {
        if (userId == null) return;
        students.deleteOne(eq("userId", userId));
        userRepository.deleteById(userId.intValue());
    }

    public StudentEntity findStudentByObjectId(ObjectId studentObjectId) {
        Document doc = students.find(eq("_id", studentObjectId)).first();
        return doc == null ? null : mapToStudent(doc);
    }

    public ObjectId studentObjectIdByUserId(Long userId) {
        Document doc = students.find(eq("userId", userId)).first();
        if (doc == null) {
            throw new IllegalArgumentException("El estudiante no existe");
        }
        return doc.getObjectId("_id");
    }

    public Long userIdByStudentObjectId(ObjectId studentObjectId) {
        Document doc = students.find(eq("_id", studentObjectId)).first();
        if (doc == null) return null;
        Number userId = doc.get("userId", Number.class);
        return userId == null ? null : userId.longValue();
    }

    public List<SubjectStatusDTO> findCurriculum(Long userId) {
        ObjectId studentObjectId = studentObjectIdByUserId(userId);
        Document studentDoc = students.find(eq("_id", studentObjectId)).first();
        String careerCode = studentDoc == null ? null : studentDoc.getString("careerCode");

        Map<ObjectId, Document> subjectMap = new HashMap<>();
        for (Document sub : subjects.find()) subjectMap.put(sub.getObjectId("_id"), sub);

        List<Document> pipeline = List.of(
                new Document("$match",
                        new Document("studentId", studentObjectId)
                                .append("status", new Document("$in", List.of("ACTIVE", "COMPLETED")))),
                new Document("$lookup",
                        new Document("from", "sections")
                                .append("localField", "sectionId")
                                .append("foreignField", "_id")
                                .append("as", "section")),
                new Document("$unwind", "$section"),
                new Document("$lookup",
                        new Document("from", "subjects")
                                .append("localField", "section.subjectId")
                                .append("foreignField", "_id")
                                .append("as", "subject")),
                new Document("$unwind", "$subject"),
                new Document("$lookup",
                        new Document("from", "semesters")
                                .append("localField", "section.semesterId")
                                .append("foreignField", "_id")
                                .append("as", "semester")),
                new Document("$unwind",
                        new Document("path", "$semester").append("preserveNullAndEmptyArrays", true)),
                new Document("$lookup",
                        new Document("from", "grades")
                                .append("localField", "_id")
                                .append("foreignField", "enrollmentId")
                                .append("as", "grade")),
                new Document("$unwind",
                        new Document("path", "$grade").append("preserveNullAndEmptyArrays", true)),
                new Document("$project",
                        new Document("_id", 0)
                                .append("subjectId", new Document("$toString", "$subject._id"))
                                .append("subjectCode", "$subject.code")
                                .append("subjectName", "$subject.name")
                                .append("credits", "$subject.credits")
                                .append("enrollmentStatus", "$status")
                                .append("gradeValue", "$grade.value")
                                .append("semesterId", new Document("$toString", "$semester._id"))
                                .append("semesterYear", "$semester.year")
                                .append("semesterPeriod", "$semester.period"))
        );

        List<SubjectStatusDTO> rows = new ArrayList<>();
        for (Document row : enrollments.aggregate(pipeline)) {
            Number gradeNumber = row.get("gradeValue", Number.class);
            Double grade = gradeNumber == null ? null : gradeNumber.doubleValue();

            String enrollmentStatus = row.getString("enrollmentStatus");
            String status;
            if ("ACTIVE".equals(enrollmentStatus)) {
                status = "ENROLLED";
            } else if (grade != null && grade >= 4.0) {
                status = "APPROVED";
            } else if (grade != null) {
                status = "FAILED";
            } else {
                status = "PENDING";
            }

            SubjectStatusDTO dto = new SubjectStatusDTO();
            dto.setSubjectId(row.getString("subjectId"));
            dto.setSubjectCode(row.getString("subjectCode"));
            dto.setSubjectName(row.getString("subjectName"));
            Number credits = row.get("credits", Number.class);
            dto.setCredits(credits == null ? 0 : credits.intValue());
            dto.setStatus(status);
            dto.setGrade(grade);
            dto.setSemesterId(row.getString("semesterId"));
            Number year = row.get("semesterYear", Number.class);
            dto.setSemesterYear(year == null ? null : year.intValue());
            dto.setSemesterPeriod(row.getString("semesterPeriod"));
            rows.add(dto);
        }

        for (Document subject : subjectMap.values()) {
            String subjectCareer = subject.getString("careerCode");
            if (careerCode != null && !careerCode.equals(subjectCareer)) continue;
            boolean attempted = rows.stream()
                    .anyMatch(r -> r.getSubjectId().equals(subject.getObjectId("_id").toHexString()));
            if (attempted) continue;
            SubjectStatusDTO dto = new SubjectStatusDTO();
            dto.setSubjectId(subject.getObjectId("_id").toHexString());
            dto.setSubjectCode(subject.getString("code"));
            dto.setSubjectName(subject.getString("name"));
            dto.setCredits(subject.getInteger("credits", 0));
            dto.setStatus("PENDING");
            rows.add(dto);
        }

        rows.sort(Comparator
                .comparing(SubjectStatusDTO::getSemesterYear,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(dto -> semesterPeriodRank(dto.getSemesterPeriod()),
                        Comparator.reverseOrder())
                .thenComparing(dto -> dto.getSubjectCode() == null ? "" : dto.getSubjectCode()));
        return rows;
    }

    private static int semesterPeriodRank(String period) {
        if ("2S".equals(period)) return 2;
        if ("1S".equals(period)) return 1;
        return 0;
    }

    private static String validRut(String rut) {
        if (rut != null && rut.matches("^[0-9]{7,8}-[0-9Kk]{1}$")) {
            return rut;
        }
        int digits = ThreadLocalRandom.current().nextInt(10_000_000, 99_999_999);
        return digits + "-K";
    }

    public static StudentEntity mapToStudent(Document doc) {
        Number userId = doc.get("userId", Number.class);
        StudentEntity entity = new StudentEntity();
        entity.setId(userId == null ? null : userId.longValue());
        entity.setUsuarioId(userId == null ? null : userId.longValue());
        entity.setEnrollmentNumber(doc.getString("enrollmentNumber"));
        entity.setFirstName(doc.getString("firstName"));
        entity.setLastName(doc.getString("lastName"));
        entity.setCareerCode(doc.getString("careerCode"));
        entity.setAcademicStatus(doc.getString("academicStatus"));
        return entity;
    }

    public static LocalDate dateToLocalDate(Date date) {
        if (date == null) return null;
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneOffset.UTC).toLocalDate();
    }
}
