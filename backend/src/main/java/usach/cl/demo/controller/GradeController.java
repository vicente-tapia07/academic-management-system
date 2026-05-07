package usach.cl.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import usach.cl.demo.dto.GradeDTO;
import usach.cl.demo.model.GradeEntity;
import usach.cl.demo.service.GradeService;
import java.util.List;

@RestController
@RequestMapping("/api/grades")
public class GradeController {

    private final GradeService gradeService;

    public GradeController(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    @GetMapping
    public ResponseEntity<List<GradeEntity>> getAll() {
        return ResponseEntity.ok(gradeService.getAll());
    }

    @PostMapping
    public ResponseEntity<GradeEntity> create(@RequestBody GradeEntity grade) {
        return ResponseEntity.ok(gradeService.save(grade));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GradeEntity> update(@PathVariable Long id, @RequestBody GradeEntity grade) {
        grade.setId(id);
        return ResponseEntity.ok(gradeService.save(grade));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<GradeDTO>> getMyGrades(@PathVariable Long studentId) {
        return ResponseEntity.ok(gradeService.getGradesByStudent(studentId));
    }
}