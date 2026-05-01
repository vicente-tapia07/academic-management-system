package com.usach.academic.controller;

import com.usach.academic.model.CareerEntity;
import com.usach.academic.model.SubjectEntity;
import com.usach.academic.service.CareerService;
import com.usach.academic.service.SubjectService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// controlador que expone los endpoints REST para carreras
@RestController
@RequestMapping("/api/careers")
public class CareerController {

    private final CareerService careerService;
    private final SubjectService subjectService;

    public CareerController(CareerService careerService, SubjectService subjectService) {
        this.careerService = careerService;
        this.subjectService = subjectService;
    }

    // GET /api/careers - retorna todas las carreras
    @GetMapping
    public ResponseEntity<List<CareerEntity>> findAll() {
        return ResponseEntity.ok(careerService.findAll());
    }

    // GET /api/careers/{id} - retorna una carrera por id
    @GetMapping("/{id}")
    public ResponseEntity<CareerEntity> findById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(careerService.findById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // GET /api/careers/{id}/subjects - retorna las asignaturas de una carrera
    @GetMapping("/{id}/subjects")
    public ResponseEntity<List<SubjectEntity>> findSubjectsByCareer(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(subjectService.findByCareerId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // POST /api/careers - crea una nueva carrera
    @PostMapping
    public ResponseEntity<CareerEntity> save(@RequestBody CareerEntity career) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(careerService.save(career));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // PUT /api/careers/{id} - actualiza una carrera existente
    @PutMapping("/{id}")
    public ResponseEntity<CareerEntity> update(@PathVariable Long id,
                                               @RequestBody CareerEntity career) {
        try {
            return ResponseEntity.ok(careerService.update(id, career));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE /api/careers/{id} - elimina una carrera
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            careerService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}