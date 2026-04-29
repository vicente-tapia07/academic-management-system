package com.usach.academic.controller;

import com.usach.academic.model.CarreraEntity;
import com.usach.academic.service.CarreraService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Controlador que expone los endpoints REST para carreras
@RestController
@RequestMapping("/api/carreras")
public class CarreraController {

    private final CarreraService carreraService;

    public CarreraController(CarreraService carreraService) {
        this.carreraService = carreraService;
    }

    // GET /api/carreras - retorna todas las carreras
    @GetMapping
    public ResponseEntity<List<CarreraEntity>> findAll() {
        return ResponseEntity.ok(carreraService.findAll());
    }

    // GET /api/carreras/{id} - retorna una carrera por id
    @GetMapping("/{id}")
    public ResponseEntity<CarreraEntity> findById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(carreraService.findById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // POST /api/carreras - crea una nueva carrera
    @PostMapping
    public ResponseEntity<CarreraEntity> save(@RequestBody CarreraEntity carrera) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(carreraService.save(carrera));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // PUT /api/carreras/{id} - actualiza una carrera existente
    @PutMapping("/{id}")
    public ResponseEntity<CarreraEntity> update(@PathVariable Long id,
                                                @RequestBody CarreraEntity carrera) {
        try {
            return ResponseEntity.ok(carreraService.update(id, carrera));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE /api/carreras/{id} - elimina una carrera
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            carreraService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}