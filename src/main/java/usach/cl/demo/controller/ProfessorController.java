package usach.cl.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import usach.cl.demo.entity.Professor;
import usach.cl.demo.model.ProfessorDto;
import usach.cl.demo.model.FailureRateDTO;
import usach.cl.demo.model.GradeEntity;
import usach.cl.demo.service.ProfessorService;

import java.util.List;

@RestController
@RequestMapping("/api/professors")
public class ProfessorController {

    private final ProfessorService professorService;

    public ProfessorController(ProfessorService professorService) {
        this.professorService = professorService;
    }

    @GetMapping
    public ResponseEntity<List<Professor>> getAll() {
        return ResponseEntity.ok(professorService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Professor> getById(@PathVariable int id) {
        return ResponseEntity.ok(professorService.getById(id));
    }

    @PostMapping
    public ResponseEntity<Professor> create(@RequestBody ProfessorDto dto) throws Exception {
        return ResponseEntity.ok(professorService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Professor> update(@PathVariable int id, @RequestBody ProfessorDto dto) {
        return ResponseEntity.ok(professorService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        professorService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reports")
    public List<FailureRateDTO> getReports() {
        return professorService.getFailureReport();
    }

    @PostMapping("/grade")
    public GradeEntity submitGrade(@RequestBody GradeEntity grade, @RequestParam String professorRut) {
        // En una app real, el RUT del profesor se saca del Token JWT (Keycloak)
        // Por ahora, lo pasamos por la URL para probar que la Auditoría funciona.
        return professorService.saveGrade(grade, professorRut);
    }
}