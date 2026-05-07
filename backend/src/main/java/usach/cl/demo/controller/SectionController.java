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

    @PostMapping
    public ResponseEntity<SectionEntity> save(@RequestBody SectionEntity section) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(sectionService.save(section));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}