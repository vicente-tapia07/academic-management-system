package usach.cl.demo.service;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

@Service
public class AuthorizationService {

    private final MongoCollection<Document> users;
    private final MongoCollection<Document> students;
    private final MongoCollection<Document> professors;
    private final MongoCollection<Document> sections;
    private final MongoCollection<Document> enrollments;

    public AuthorizationService(MongoDatabase mongoDatabase) {
        this.users = mongoDatabase.getCollection("users");
        this.students = mongoDatabase.getCollection("students");
        this.professors = mongoDatabase.getCollection("professors");
        this.sections = mongoDatabase.getCollection("sections");
        this.enrollments = mongoDatabase.getCollection("enrollments");
    }

    public boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    public void requireStudentAccess(Authentication authentication, Long studentId) {
        if (isAdmin(authentication)) return;
        requireAuthenticated(authentication);
        if (studentId == null) deny();
        if (students.find(eq("userId", studentId)).first() == null) deny();
        if (users.find(and(eq("id", studentId.intValue()), eq("email", authentication.getName()))).first() == null) deny();
    }

    public void requireMongoStudentAccess(Authentication authentication, Long userId) {
        if (isAdmin(authentication)) return;
        requireAuthenticated(authentication);
        if (userId == null) deny();
        if (users.find(and(eq("id", userId.intValue()),
                eq("email", authentication.getName()),
                eq("rol", "STUDENT"))).first() == null) deny();
    }

    public void requireProfessorAccess(Authentication authentication, Long professorId) {
        if (isAdmin(authentication)) return;
        requireAuthenticated(authentication);
        if (professorId == null) deny();
        if (professors.find(eq("userId", professorId)).first() == null) deny();
        if (users.find(and(eq("id", professorId.intValue()), eq("email", authentication.getName()))).first() == null) deny();
    }

    public void requireEnrollmentStudentAccess(Authentication authentication, String enrollmentId) {
        if (isAdmin(authentication)) return;
        requireAuthenticated(authentication);
        if (enrollmentId == null || !ObjectId.isValid(enrollmentId)) deny();
        Document enrollment = enrollments.find(eq("_id", new ObjectId(enrollmentId))).first();
        if (enrollment == null) deny();
        Document student = students.find(eq("_id", enrollment.getObjectId("studentId"))).first();
        if (student == null) deny();
        Number userId = student.get("userId", Number.class);
        if (userId == null) deny();
        if (users.find(and(eq("id", userId.intValue()), eq("email", authentication.getName()))).first() == null) deny();
    }

    public void requireEnrollmentReadAccess(Authentication authentication, String enrollmentId) {
        if (isAdmin(authentication)) return;
        requireAuthenticated(authentication);
        if (enrollmentId == null || !ObjectId.isValid(enrollmentId)) deny();
        Document enrollment = enrollments.find(eq("_id", new ObjectId(enrollmentId))).first();
        if (enrollment == null) deny();
        String email = authentication.getName();

        Document student = students.find(eq("_id", enrollment.getObjectId("studentId"))).first();
        if (student != null) {
            Number userId = student.get("userId", Number.class);
            if (userId != null && users.find(and(eq("id", userId.intValue()), eq("email", email))).first() != null) {
                return;
            }
        }
        Document section = sections.find(eq("_id", enrollment.getObjectId("sectionId"))).first();
        if (section != null) {
            String professorId = section.getString("professorId");
            if (professorId != null && professorId.matches("\\d+")) {
                int professorUserId = Integer.parseInt(professorId);
                if (users.find(and(eq("id", professorUserId), eq("email", email))).first() != null) {
                    return;
                }
            }
        }
        deny();
    }

    public void requireProfessorOwnsSection(Authentication authentication, String sectionId) {
        if (isAdmin(authentication)) return;
        requireAuthenticated(authentication);
        if (sectionId == null || !ObjectId.isValid(sectionId)) deny();
        Document section = sections.find(eq("_id", new ObjectId(sectionId))).first();
        if (section == null) deny();
        String professorId = section.getString("professorId");
        if (professorId == null || !professorId.matches("\\d+")) deny();
        if (users.find(and(eq("id", Integer.parseInt(professorId)), eq("email", authentication.getName()))).first() == null) deny();
    }

    public void requireProfessorOwnsEnrollment(Authentication authentication, String enrollmentId) {
        if (isAdmin(authentication)) return;
        requireAuthenticated(authentication);
        if (enrollmentId == null || !ObjectId.isValid(enrollmentId)) deny();
        Document enrollment = enrollments.find(eq("_id", new ObjectId(enrollmentId))).first();
        if (enrollment == null) deny();
        Document section = sections.find(eq("_id", enrollment.getObjectId("sectionId"))).first();
        if (section == null) deny();
        String professorId = section.getString("professorId");
        if (professorId == null || !professorId.matches("\\d+")) deny();
        if (users.find(and(eq("id", Integer.parseInt(professorId)), eq("email", authentication.getName()))).first() == null) deny();
    }

    public void requireProfessorRut(Authentication authentication, String professorRut) {
        if (isAdmin(authentication)) return;
        requireAuthenticated(authentication);
        if (professorRut == null || professorRut.isBlank()) deny();
        if (users.find(and(eq("rut", professorRut),
                eq("email", authentication.getName()),
                eq("rol", "PROFESSOR"))).first() == null) deny();
    }

    public void requireAuthenticated(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) deny();
    }

    private void deny() {
        throw new AccessDeniedException("Acceso denegado");
    }
}
