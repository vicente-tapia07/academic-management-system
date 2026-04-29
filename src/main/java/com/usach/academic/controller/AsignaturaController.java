package com.usach.academic.controller;

import com.usach.academic.model.AsignaturaEntity;
import com.usach.academic.service.AsignaturaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Controlador que expone los endpoints REST para asignaturas
@RestController
@RequestMapping("/api/asignaturas")
public class AsignaturaController {

    private final AsignaturaService asignaturaService;

    public AsignaturaController(AsignaturaService asignaturaService) {
        this.asignaturaService = asignaturaService;
    }

    // GET /api/asignaturas - retorna todas las asignaturas
    @GetMapping
    public ResponseEntity<List<AsignaturaEntity>> findAll() {
        return ResponseEntity.ok(asignaturaService.findAll());
    }

    // GET /api/asignaturas/{id} - retorna una asignatura por id
    @GetMapping("/{id}")
    public ResponseEntity<AsignaturaEntity> findById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(asignaturaService.findById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // POST /api/asignaturas - crea una nueva asignatura
    @PostMapping
    public ResponseEntity<AsignaturaEntity> save(@RequestBody AsignaturaEntity asignatura) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(asignaturaService.save(asignatura));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // PUT /api/asignaturas/{id} - actualiza una asignatura existente
    @PutMapping("/{id}")
    public ResponseEntity<AsignaturaEntity> update(@PathVariable Long id,
                                                   @RequestBody AsignaturaEntity asignatura) {
        try {
            return ResponseEntity.ok(asignaturaService.update(id, asignatura));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE /api/asignaturas/{id} - elimina una asignatura
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            asignaturaService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
