package usach.cl.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import usach.cl.demo.model.GradeEntity;
import usach.cl.demo.repository.GradeRepository;

import java.util.List;

@RestController
@RequestMapping("/api/grades")
public class GradeController {

    private final GradeRepository gradeRepository;

    public GradeController(GradeRepository gradeRepository) {
        this.gradeRepository = gradeRepository;
    }

    // GET /api/grades — retorna todas las notas
    @GetMapping
    public ResponseEntity<List<GradeEntity>> getAll() {
        List<GradeEntity> grades = (List<GradeEntity>) gradeRepository.findAll();
        return ResponseEntity.ok(grades);
    }

    // POST /api/grades — crea una nota nueva
    @PostMapping
    public ResponseEntity<GradeEntity> create(@RequestBody GradeEntity grade) {
        GradeEntity saved = gradeRepository.save(grade);
        return ResponseEntity.ok(saved);
    }

    // PUT /api/grades/{id} — edita una nota existente
    @PutMapping("/{id}")
    public ResponseEntity<GradeEntity> update(@PathVariable Long id, @RequestBody GradeEntity grade) {
        grade.setId(id);
        GradeEntity saved = gradeRepository.save(grade);
        return ResponseEntity.ok(saved);
    }
}