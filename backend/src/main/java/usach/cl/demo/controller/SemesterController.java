package usach.cl.demo.controller;

import usach.cl.demo.model.SemesterEntity;
import usach.cl.demo.service.SemesterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// controlador que expone los endpoints REST para semestres
@RestController
@RequestMapping("/api/semesters")
public class SemesterController {

    private final SemesterService semesterService;

    public SemesterController(SemesterService semesterService) {
        this.semesterService = semesterService;
    }

    // GET /api/semesters - retorna todos los semestres
    @GetMapping
    public ResponseEntity<List<SemesterEntity>> findAll() {
        return ResponseEntity.ok(semesterService.findAll());
    }

    // GET /api/semesters/{id} - retorna un semestre por id
    @GetMapping("/{id}")
    public ResponseEntity<SemesterEntity> findById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(semesterService.findById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // POST /api/semesters - crea un nuevo semestre
    @PostMapping
    public ResponseEntity<SemesterEntity> save(@RequestBody SemesterEntity semester) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(semesterService.save(semester));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // PUT /api/semesters/{id} - actualiza un semestre existente
    @PutMapping("/{id}")
    public ResponseEntity<SemesterEntity> update(@PathVariable Long id,
                                                 @RequestBody SemesterEntity semester) {
        try {
            return ResponseEntity.ok(semesterService.update(id, semester));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // POST /api/semesters/{id}/close - llama al SP de cierre de semestre
    @PostMapping("/{id}/close")
    public ResponseEntity<String> closeSemester(@PathVariable Long id) {
        try {
            semesterService.closeSemester(id);
            return ResponseEntity.ok("Semester closed successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}