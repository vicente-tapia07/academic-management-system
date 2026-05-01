package usach.cl.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import usach.cl.demo.model.EnrollmentEntity;
import usach.cl.demo.service.EnrollmentService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    // Spring inyecta el servicio automáticamente
    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    // Retorna todas las inscripciones
    @GetMapping
    public ResponseEntity<List<EnrollmentEntity>> getAll() {
        List<EnrollmentEntity> enrollments = enrollmentService.findAll();
        return ResponseEntity.ok(enrollments);
    }

    // Retorna una inscripción por su ID
    @GetMapping("/{id}")
    public ResponseEntity<EnrollmentEntity> getById(@PathVariable Long id) {
        Optional<EnrollmentEntity> enrollment = enrollmentService.findById(id);
        return enrollment
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Retorna todas las inscripciones de un estudiante
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<EnrollmentEntity>> getByStudentId(@PathVariable Long studentId) {
        List<EnrollmentEntity> enrollments = enrollmentService.findByStudentId(studentId);
        return ResponseEntity.ok(enrollments);
    }

    // Crea una nueva inscripción
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

    // Actualiza el estado de una inscripción
    @PatchMapping("/{id}/status")
    public ResponseEntity<String> updateStatus(@PathVariable Long id, @RequestBody String status) {
        String cleanStatus = status.replace("\"", "").trim();
        int result = enrollmentService.updateStatus(id, cleanStatus);
        if (result > 0) {
            return ResponseEntity.ok("Status updated successfully");
        }
        return ResponseEntity.notFound().build();
    }

    // Elimina una inscripción por su ID
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        int result = enrollmentService.deleteById(id);
        if (result > 0) {
            return ResponseEntity.ok("Enrollment deleted successfully");
        }
        return ResponseEntity.notFound().build();
    }
}