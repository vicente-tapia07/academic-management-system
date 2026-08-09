package usach.cl.demo.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Repository;
import usach.cl.demo.model.EnrollmentEntity;
import usach.cl.demo.service.mongo.EnrollmentTransactionService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;

@Repository
public class MongoEnrollmentRepository {

    private final MongoCollection<Document> enrollments;
    private final MongoCollection<Document> grades;
    private final MongoCollection<Document> students;
    private final MongoStudentRepository studentRepository;
    private final EnrollmentTransactionService transactionService;

    public MongoEnrollmentRepository(MongoDatabase mongoDatabase,
                                     MongoStudentRepository studentRepository,
                                     EnrollmentTransactionService transactionService) {
        this.enrollments = mongoDatabase.getCollection("enrollments");
        this.grades = mongoDatabase.getCollection("grades");
        this.students = mongoDatabase.getCollection("students");
        this.studentRepository = studentRepository;
        this.transactionService = transactionService;
    }

    public List<EnrollmentEntity> findAll() {
        Map<ObjectId, Long> userIdByStudent = studentUserIdMap();
        List<EnrollmentEntity> result = new ArrayList<>();
        for (Document doc : enrollments.find()) {
            result.add(mapToEnrollment(doc, userIdByStudent));
        }
        return result;
    }

    public Optional<EnrollmentEntity> findById(String id) {
        if (id == null || !ObjectId.isValid(id)) return Optional.empty();
        Document doc = enrollments.find(eq("_id", new ObjectId(id))).first();
        if (doc == null) return Optional.empty();
        return Optional.of(mapToEnrollment(doc, studentUserIdMap()));
    }

    public List<EnrollmentEntity> findByStudentId(Long studentId) {
        Map<ObjectId, Long> userIdByStudent = studentUserIdMap();
        ObjectId studentObjectId = studentRepository.studentObjectIdByUserId(studentId);
        List<EnrollmentEntity> result = new ArrayList<>();
        for (Document doc : enrollments.find(eq("studentId", studentObjectId))) {
            result.add(mapToEnrollment(doc, userIdByStudent));
        }
        return result;
    }

    public List<EnrollmentEntity> findBySectionId(String sectionId) {
        if (sectionId == null || !ObjectId.isValid(sectionId)) return new ArrayList<>();
        Map<ObjectId, Long> userIdByStudent = studentUserIdMap();
        List<EnrollmentEntity> result = new ArrayList<>();
        for (Document doc : enrollments.find(eq("sectionId", new ObjectId(sectionId)))) {
            result.add(mapToEnrollment(doc, userIdByStudent));
        }
        return result;
    }

    public void enrollStudent(Long studentId, String sectionId) {
        ObjectId studentObjectId = studentRepository.studentObjectIdByUserId(studentId);
        transactionService.enroll(studentObjectId.toHexString(), sectionId);
    }

    public boolean cancelAndRestoreSeat(String enrollmentId) {
        if (enrollmentId == null || !ObjectId.isValid(enrollmentId)) return false;
        Document doc = enrollments.find(eq("_id", new ObjectId(enrollmentId))).first();
        if (doc == null || !"ACTIVE".equals(doc.getString("status"))) return false;
        transactionService.cancelAndRestoreSeat(enrollmentId);
        return true;
    }

    public boolean hasGrade(String enrollmentId) {
        if (enrollmentId == null || !ObjectId.isValid(enrollmentId)) return false;
        Document grade = grades.find(eq("enrollmentId", new ObjectId(enrollmentId))).first();
        return grade != null;
    }

    public int updateStatus(String id, String status) {
        if (id == null || !ObjectId.isValid(id)) return 0;
        Document updates = new Document("status", status).append("updatedAt", new Date());
        if ("COMPLETED".equals(status)) {
            updates.append("completedAt", new Date());
        }
        return (int) enrollments.updateOne(eq("_id", new ObjectId(id)),
                new Document("$set", updates)).getModifiedCount();
    }

    private Map<ObjectId, Long> studentUserIdMap() {
        Map<ObjectId, Long> map = new HashMap<>();
        for (Document doc : students.find()) {
            Number userId = doc.get("userId", Number.class);
            if (userId != null) {
                map.put(doc.getObjectId("_id"), userId.longValue());
            }
        }
        return map;
    }

    private static EnrollmentEntity mapToEnrollment(Document doc, Map<ObjectId, Long> userIdByStudent) {
        EnrollmentEntity enrollment = new EnrollmentEntity();
        enrollment.setId(doc.getObjectId("_id").toHexString());
        ObjectId studentObjectId = doc.getObjectId("studentId");
        enrollment.setStudentId(userIdByStudent.get(studentObjectId));
        enrollment.setSectionId(doc.getObjectId("sectionId").toHexString());
        enrollment.setEnrollmentDate(toLocalDate(doc.getDate("enrolledAt")));
        enrollment.setStatus(doc.getString("status"));
        return enrollment;
    }

    private static LocalDate toLocalDate(Date date) {
        if (date == null) return null;
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneOffset.UTC).toLocalDate();
    }
}
