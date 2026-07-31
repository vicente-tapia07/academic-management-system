package usach.cl.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import usach.cl.demo.dto.GradeDTO;
import usach.cl.demo.model.GradeEntity;
import usach.cl.demo.service.GradeService;
import usach.cl.demo.service.AuthorizationService;
import java.util.List;

@RestController
@RequestMapping("/api/grades")
public class GradeController {

    private final GradeService gradeService;
    private final AuthorizationService authorizationService;

    public GradeController(GradeService gradeService, AuthorizationService authorizationService) {
        this.gradeService = gradeService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public ResponseEntity<List<GradeEntity>> getAll() {
        return ResponseEntity.ok(gradeService.getAll());
    }

    @PostMapping
    public ResponseEntity<GradeEntity> create(@RequestBody GradeEntity grade,
                                               Authentication authentication) {
        authorizationService.requireProfessorOwnsEnrollment(authentication, grade.getEnrollmentId());
        return ResponseEntity.ok(gradeService.save(grade));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GradeEntity> update(@PathVariable Long id, @RequestBody GradeEntity grade,
                                               Authentication authentication) {
        authorizationService.requireProfessorOwnsEnrollment(authentication, grade.getEnrollmentId());
        grade.setId(id);
        return ResponseEntity.ok(gradeService.save(grade));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<GradeDTO>> getMyGrades(@PathVariable Long studentId,
                                                       Authentication authentication) {
        authorizationService.requireStudentAccess(authentication, studentId);
        return ResponseEntity.ok(gradeService.getGradesByStudent(studentId));
    }
}
