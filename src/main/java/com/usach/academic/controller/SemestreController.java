package com.usach.academic.controller;

import com.usach.academic.model.SemestreEntity;
import com.usach.academic.service.SemestreService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Controlador que expone los endpoints REST para semestres
@RestController
@RequestMapping("/api/semestres")
public class SemestreController {

    private final SemestreService semestreService;

    public SemestreController(SemestreService semestreService) {
        this.semestreService = semestreService;
    }

    // GET /api/semestres - retorna todos los semestres
    @GetMapping
    public ResponseEntity<List<SemestreEntity>> findAll() {
        return ResponseEntity.ok(semestreService.findAll());
    }

    // GET /api/semestres/{id} - retorna un semestre por id
    @GetMapping("/{id}")
    public ResponseEntity<SemestreEntity> findById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(semestreService.findById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // POST /api/semestres - crea un nuevo semestre
    @PostMapping
    public ResponseEntity<SemestreEntity> save(@RequestBody SemestreEntity semestre) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(semestreService.save(semestre));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // PUT /api/semestres/{id} - actualiza un semestre existente
    @PutMapping("/{id}")
    public ResponseEntity<SemestreEntity> update(@PathVariable Long id,
                                                 @RequestBody SemestreEntity semestre) {
        try {
            return ResponseEntity.ok(semestreService.update(id, semestre));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // POST /api/semestres/{id}/cierre - llama al SP de cierre de semestre
    @PostMapping("/{id}/cierre")
    public ResponseEntity<String> cerrarSemestre(@PathVariable Long id) {
        try {
            semestreService.cerrarSemestre(id);
            return ResponseEntity.ok("Semestre cerrado correctamente");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}