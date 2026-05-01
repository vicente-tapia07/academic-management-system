package com.usach.academic.controller;

import com.usach.academic.model.SubjectEntity;
import com.usach.academic.service.SubjectService;
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

    // GET /api/subjects - retorna todas las asignaturas
    @GetMapping
    public ResponseEntity<List<SubjectEntity>> findAll() {
        return ResponseEntity.ok(subjectService.findAll());
    }

    // GET /api/subjects/{id} - retorna una asignatura por id
    @GetMapping("/{id}")
    public ResponseEntity<SubjectEntity> findById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(subjectService.findById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // POST /api/subjects - crea una nueva asignatura
    @PostMapping
    public ResponseEntity<SubjectEntity> save(@RequestBody SubjectEntity subject) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(subjectService.save(subject));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // PUT /api/subjects/{id} - actualiza una asignatura existente
    @PutMapping("/{id}")
    public ResponseEntity<SubjectEntity> update(@PathVariable Long id,
                                                @RequestBody SubjectEntity subject) {
        try {
            return ResponseEntity.ok(subjectService.update(id, subject));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE /api/subjects/{id} - elimina una asignatura
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            subjectService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}