package usach.cl.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import usach.cl.demo.model.EnrollmentEntity;
import usach.cl.demo.service.EnrollmentService;
import usach.cl.demo.service.AuthorizationService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final AuthorizationService authorizationService;

    public EnrollmentController(EnrollmentService enrollmentService,
                                AuthorizationService authorizationService) {
        this.enrollmentService = enrollmentService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public ResponseEntity<List<EnrollmentEntity>> getAll() {
        List<EnrollmentEntity> enrollments = enrollmentService.findAll();
        return ResponseEntity.ok(enrollments);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EnrollmentEntity> getById(@PathVariable String id,
                                                     Authentication authentication) {
        authorizationService.requireEnrollmentReadAccess(authentication, id);
        Optional<EnrollmentEntity> enrollment = enrollmentService.findById(id);
        return enrollment
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<EnrollmentEntity>> getByStudentId(@PathVariable Long studentId,
                                                                  Authentication authentication) {
        authorizationService.requireStudentAccess(authentication, studentId);
        List<EnrollmentEntity> enrollments = enrollmentService.findByStudentId(studentId);
        return ResponseEntity.ok(enrollments);
    }

    @GetMapping("/section/{sectionId}")
    public ResponseEntity<List<EnrollmentEntity>> getBySectionId(@PathVariable String sectionId,
                                                                  Authentication authentication) {
        authorizationService.requireProfessorOwnsSection(authentication, sectionId);
        List<EnrollmentEntity> enrollments = enrollmentService.findBySectionId(sectionId);
        return ResponseEntity.ok(enrollments);
    }

    @PostMapping
    public ResponseEntity<String> create(@RequestBody EnrollmentEntity enrollment,
                                         Authentication authentication) {
        return enroll(enrollment, authentication);
    }

    @PostMapping("/enroll")
    public ResponseEntity<String> enroll(@RequestBody EnrollmentEntity enrollment,
                                         Authentication authentication) {
        if (enrollment.getStudentId() == null || enrollment.getSectionId() == null) {
            throw new IllegalArgumentException("studentId y sectionId son obligatorios");
        }
        authorizationService.requireStudentAccess(authentication, enrollment.getStudentId());
        try {
            enrollmentService.enrollStudent(
                    enrollment.getStudentId(),
                    enrollment.getSectionId()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body("Student enrolled successfully");
        } catch (Exception e) {
            String cleanMessage = e.getMessage() == null ? "Error al inscribir" : e.getMessage();
            return ResponseEntity.badRequest().body(cleanMessage);
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<String> updateStatus(@PathVariable String id, @RequestBody String status) {
        String cleanStatus = status.replace("\"", "").trim();
        int result = enrollmentService.updateStatus(id, cleanStatus);
        if (result > 0) {
            return ResponseEntity.ok("Status updated successfully");
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable String id,
                                         Authentication authentication) {
        authorizationService.requireEnrollmentStudentAccess(authentication, id);
        Optional<EnrollmentEntity> enrollment = enrollmentService.findById(id);
        if (enrollment.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!"ACTIVE".equals(enrollment.get().getStatus())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Solo se puede cancelar una inscripción activa");
        }

        boolean cancelled = enrollmentService.cancelEnrollment(id);
        if (cancelled) {
            return ResponseEntity.ok("Inscripción cancelada y cupo restaurado");
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("La inscripción ya no se encuentra activa");
    }
}
