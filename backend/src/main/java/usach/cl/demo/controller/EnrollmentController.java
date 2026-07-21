package usach.cl.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import usach.cl.demo.dto.NearbySectionResponse;
import usach.cl.demo.model.EnrollmentEntity;
import usach.cl.demo.service.EnrollmentService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @GetMapping
    public ResponseEntity<List<EnrollmentEntity>> getAll() {
        List<EnrollmentEntity> enrollments = enrollmentService.findAll();
        return ResponseEntity.ok(enrollments);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EnrollmentEntity> getById(@PathVariable Long id) {
        Optional<EnrollmentEntity> enrollment = enrollmentService.findById(id);
        return enrollment
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<EnrollmentEntity>> getByStudentId(@PathVariable Long studentId) {
        List<EnrollmentEntity> enrollments = enrollmentService.findByStudentId(studentId);
        return ResponseEntity.ok(enrollments);
    }

    @GetMapping("/section/{sectionId}")
    public ResponseEntity<List<EnrollmentEntity>> getBySectionId(@PathVariable Long sectionId) {
        List<EnrollmentEntity> enrollments = enrollmentService.findBySectionId(sectionId);
        return ResponseEntity.ok(enrollments);
    }

    @PostMapping
    public ResponseEntity<String> create(@RequestBody EnrollmentEntity enrollment) {
        enrollment.setEnrollmentDate(LocalDate.now());
        enrollment.setStatus("ACTIVE");
        int result = enrollmentService.save(enrollment);
        if (result > 0) {
            return ResponseEntity.status(HttpStatus.CREATED).body("Enrollment created successfully");
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating enrollment");
    }

    @PostMapping("/enroll")
    public ResponseEntity<String> enroll(@RequestBody EnrollmentEntity enrollment) {
        try {
            enrollmentService.enrollStudent(
                    enrollment.getStudentId(),
                    enrollment.getSectionId()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body("Student enrolled successfully");
        } catch (Exception e) {
            String fullMessage = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();

            if (fullMessage.contains("enrollment_student_id_section_id_key")) {
                return ResponseEntity.badRequest().body("Ya estás inscrito en esta asignatura");
            }

            String cleanMessage = fullMessage.split("\n")[0].replace("ERROR: ", "").trim();
            return ResponseEntity.badRequest().body(cleanMessage);
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<String> updateStatus(@PathVariable Long id, @RequestBody String status) {
        String cleanStatus = status.replace("\"", "").trim();
        int result = enrollmentService.updateStatus(id, cleanStatus);
        if (result > 0) {
            return ResponseEntity.ok("Status updated successfully");
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        boolean cancelled = enrollmentService.cancelEnrollment(id);
        if (cancelled) {
            return ResponseEntity.ok("Inscripción cancelada y cupo restaurado");
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/nearby-sections")
    public List<NearbySectionResponse> getNearbySections(
            @RequestParam Long subjectId,
            @RequestParam Double lat,
            @RequestParam Double lng) {
        return enrollmentService.findNearbySections(subjectId, lat, lng);
    }
}