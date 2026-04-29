package com.usach.academic.controller;

import com.usach.academic.model.SeccionEntity;
import com.usach.academic.service.SeccionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Controlador que expone los endpoints REST para secciones
@RestController
@RequestMapping("/api/secciones")
public class SeccionController {

    private final SeccionService seccionService;

    public SeccionController(SeccionService seccionService) {
        this.seccionService = seccionService;
    }

    // GET /api/secciones - retorna todas las secciones
    @GetMapping
    public ResponseEntity<List<SeccionEntity>> findAll() {
        return ResponseEntity.ok(seccionService.findAll());
    }

    // GET /api/secciones/{id} - retorna una seccion por id
    @GetMapping("/{id}")
    public ResponseEntity<SeccionEntity> findById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(seccionService.findById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // POST /api/secciones - crea una nueva seccion
    @PostMapping
    public ResponseEntity<SeccionEntity> save(@RequestBody SeccionEntity seccion) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(seccionService.save(seccion));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}