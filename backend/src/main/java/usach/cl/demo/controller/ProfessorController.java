package usach.cl.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import usach.cl.demo.model.ProfessorEntity;
import usach.cl.demo.dto.ProfessorDTO;
import usach.cl.demo.dto.FailureRateDTO;
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
    public ResponseEntity<List<ProfessorEntity>> getAll() {
        return ResponseEntity.ok(professorService.getAll());
    }


    @GetMapping("/{id}")
    public ResponseEntity<ProfessorEntity> getById(@PathVariable Long id) {
        return ResponseEntity.ok(professorService.getById(id));
    }


    @PostMapping
    public ResponseEntity<ProfessorEntity> create(@RequestBody ProfessorDTO dto) throws Exception {
        return ResponseEntity.ok(professorService.create(dto));
    }


    @PutMapping("/{id}")
    public ResponseEntity<ProfessorEntity> update(@PathVariable Long id, @RequestBody ProfessorDTO dto) {
        return ResponseEntity.ok(professorService.update(id, dto));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
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