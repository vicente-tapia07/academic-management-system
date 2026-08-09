package usach.cl.demo.controller.mongo;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import usach.cl.demo.dto.mongo.EnrollmentRequest;
import usach.cl.demo.model.mongo.EnrollmentDocument;
import usach.cl.demo.model.mongo.SectionDocument;
import usach.cl.demo.model.mongo.StudentDocument;
import usach.cl.demo.service.AuthorizationService;
import usach.cl.demo.service.mongo.EnrollmentTransactionException;
import usach.cl.demo.service.mongo.EnrollmentTransactionService;

import java.util.List;

@RestController
@RequestMapping("/api/mongo")
public class MongoEnrollmentController {

    private final EnrollmentTransactionService enrollmentService;
    private final AuthorizationService authorizationService;

    public MongoEnrollmentController(
            EnrollmentTransactionService enrollmentService,
            AuthorizationService authorizationService) {
        this.enrollmentService = enrollmentService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/students/{id}")
    public ResponseEntity<StudentDocument> getStudent(
            @PathVariable String id,
            Authentication authentication) {
        StudentDocument student = requireStudent(id);
        authorizationService.requireMongoStudentAccess(authentication, student.getUserId());
        return ResponseEntity.ok(student);
    }

    /**
     * Resuelve el estudiante MongoDB a partir del id de usuario del JWT.
     * GET /api/mongo/students/by-user/{userId}
     */
    @GetMapping("/students/by-user/{userId}")
    public ResponseEntity<StudentDocument> getStudentByUser(
            @PathVariable Long userId,
            Authentication authentication) {
        StudentDocument student = enrollmentService.findStudentByUser(userId)
                .orElseThrow(() -> new EnrollmentTransactionException(
                        EnrollmentTransactionException.Reason.STUDENT_NOT_FOUND,
                        "El estudiante no existe"
                ));
        authorizationService.requireMongoStudentAccess(authentication, student.getUserId());
        return ResponseEntity.ok(student);
    }

    @GetMapping("/sections")
    public ResponseEntity<List<SectionDocument>> getAvailableSections(
            @RequestParam String subjectId,
            @RequestParam String semesterId) {
        return ResponseEntity.ok(enrollmentService.findAvailableSections(subjectId, semesterId));
    }

    @PostMapping("/enrollments/enroll")
    public ResponseEntity<EnrollmentDocument> enroll(
            @RequestBody EnrollmentRequest request,
            Authentication authentication) {
        if (request == null) {
            throw new IllegalArgumentException("El cuerpo de la inscripción es obligatorio");
        }

        StudentDocument student = requireStudent(request.studentId());
        authorizationService.requireMongoStudentAccess(authentication, student.getUserId());
        EnrollmentDocument enrollment = enrollmentService.enroll(
                request.studentId(),
                request.sectionId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollment);
    }

    @GetMapping("/enrollments/student/{id}")
    public ResponseEntity<List<EnrollmentDocument>> getStudentEnrollments(
            @PathVariable String id,
            Authentication authentication) {
        StudentDocument student = requireStudent(id);
        authorizationService.requireMongoStudentAccess(authentication, student.getUserId());
        return ResponseEntity.ok(enrollmentService.findEnrollmentsByStudent(id));
    }

    private StudentDocument requireStudent(String studentId) {
        return enrollmentService.findStudent(studentId)
                .orElseThrow(() -> new EnrollmentTransactionException(
                        EnrollmentTransactionException.Reason.STUDENT_NOT_FOUND,
                        "El estudiante no existe"
                ));
    }
}
