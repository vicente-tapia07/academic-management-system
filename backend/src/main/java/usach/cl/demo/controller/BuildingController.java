package usach.cl.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import usach.cl.demo.model.BuildingEntity;
import usach.cl.demo.service.BuildingService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/buildings")
public class BuildingController {

    private final BuildingService buildingService;

    public BuildingController(BuildingService buildingService) {
        this.buildingService = buildingService;
    }

    @GetMapping
    public ResponseEntity<List<BuildingEntity>> findAll() {
        return ResponseEntity.ok(buildingService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BuildingEntity> findById(@PathVariable Long id) {
        Optional<BuildingEntity> building = buildingService.findById(id);
        return building
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<String> save(@RequestBody BuildingEntity building) {
        int result = buildingService.save(building);
        if (result > 0) {
            return ResponseEntity.status(HttpStatus.CREATED).body("Edificio creado correctamente");
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al crear el edificio");
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> update(@PathVariable Long id, @RequestBody BuildingEntity building) {
        building.setId(id);
        int result = buildingService.update(building);
        if (result > 0) {
            return ResponseEntity.ok("Edificio actualizado correctamente");
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        int result = buildingService.deleteById(id);
        if (result > 0) {
            return ResponseEntity.ok("Edificio eliminado correctamente");
        }
        return ResponseEntity.notFound().build();
    }
}