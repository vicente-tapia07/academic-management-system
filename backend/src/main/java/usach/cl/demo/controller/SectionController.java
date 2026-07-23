package usach.cl.demo.controller;

import usach.cl.demo.model.SectionEntity;
import usach.cl.demo.service.SectionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sections")
public class SectionController {

    private final SectionService sectionService;

    public SectionController(SectionService sectionService) {
        this.sectionService = sectionService;
    }

    @GetMapping
    public ResponseEntity<List<SectionEntity>> findAll() {
        return ResponseEntity.ok(sectionService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SectionEntity> findById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(sectionService.findById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Secciones activas de un estudiante
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<SectionEntity>> findByStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(sectionService.findByStudentId(studentId));
    }

    // Secciones del semestre activo de un profesor
    @GetMapping("/professor/{professorId}/active")
    public ResponseEntity<List<SectionEntity>> findByProfessorActive(@PathVariable Long professorId) {
        return ResponseEntity.ok(sectionService.findByProfessorIdAndActiveSemester(professorId));
    }

    // Todas las secciones de un profesor (para Mi Horario del profesor)
    @GetMapping("/professor/{professorId}")
    public ResponseEntity<List<SectionEntity>> findByProfessor(@PathVariable Long professorId) {
        return ResponseEntity.ok(sectionService.findByProfessorId(professorId));
    }

    @PostMapping
    public ResponseEntity<?> save(@RequestBody SectionEntity section) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(sectionService.save(section));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody SectionEntity section) {
        try {
            return ResponseEntity.ok(sectionService.update(id, section));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        try {
            sectionService.deleteById(id);
            return ResponseEntity.ok("Sección eliminada correctamente");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
