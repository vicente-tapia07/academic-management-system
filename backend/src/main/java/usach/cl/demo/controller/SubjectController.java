package usach.cl.demo.controller;

import usach.cl.demo.model.SubjectEntity;
import usach.cl.demo.service.SubjectService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// controlador que expone los endpoints REST para asignaturas
@RestController
@RequestMapping("/api/subjects")
public class SubjectController {

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @GetMapping
    public ResponseEntity<List<SubjectEntity>> findAll() {
        return ResponseEntity.ok(subjectService.findAll());
    }

    @GetMapping("/search")
    public ResponseEntity<List<SubjectEntity>> search(@RequestParam String q) {
        return ResponseEntity.ok(subjectService.search(q));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubjectEntity> findById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(subjectService.findById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<SubjectEntity> save(@RequestBody SubjectEntity subject) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(subjectService.save(subject));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubjectEntity> update(@PathVariable String id,
                                                @RequestBody SubjectEntity subject) {
        try {
            return ResponseEntity.ok(subjectService.update(id, subject));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        try {
            subjectService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
