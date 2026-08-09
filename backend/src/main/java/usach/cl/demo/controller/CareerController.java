package usach.cl.demo.controller;

import usach.cl.demo.model.CareerEntity;
import usach.cl.demo.model.SubjectEntity;
import usach.cl.demo.service.CareerService;
import usach.cl.demo.service.SubjectService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/careers")
public class CareerController {

    private final CareerService careerService;
    private final SubjectService subjectService;

    public CareerController(CareerService careerService, SubjectService subjectService) {
        this.careerService = careerService;
        this.subjectService = subjectService;
    }

    @GetMapping
    public ResponseEntity<List<CareerEntity>> findAll() {
        return ResponseEntity.ok(careerService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CareerEntity> findById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(careerService.findById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/subjects")
    public ResponseEntity<List<SubjectEntity>> findSubjectsByCareer(@PathVariable String id) {
        try {
            return ResponseEntity.ok(subjectService.findByCareerId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<CareerEntity> save(@RequestBody CareerEntity career) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(careerService.save(career));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<CareerEntity> update(@PathVariable String id,
                                               @RequestBody CareerEntity career) {
        try {
            return ResponseEntity.ok(careerService.update(id, career));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        try {
            careerService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
