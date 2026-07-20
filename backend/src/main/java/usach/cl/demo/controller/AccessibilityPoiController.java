package usach.cl.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import usach.cl.demo.model.AccessibilityPoiEntity;
import usach.cl.demo.service.AccessibilityPoiService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/accessibility-pois")
public class AccessibilityPoiController {

    private final AccessibilityPoiService accessibilityPoiService;

    public AccessibilityPoiController(AccessibilityPoiService accessibilityPoiService) {
        this.accessibilityPoiService = accessibilityPoiService;
    }

    @GetMapping
    public ResponseEntity<List<AccessibilityPoiEntity>> findAll(
            @RequestParam(required = false) Long buildingId) {
        if (buildingId != null) {
            return ResponseEntity.ok(accessibilityPoiService.findByBuildingId(buildingId));
        }
        return ResponseEntity.ok(accessibilityPoiService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccessibilityPoiEntity> findById(@PathVariable Long id) {
        Optional<AccessibilityPoiEntity> poi = accessibilityPoiService.findById(id);
        return poi
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<String> save(@RequestBody AccessibilityPoiEntity poi) {
        int result = accessibilityPoiService.save(poi);
        if (result > 0) {
            return ResponseEntity.status(HttpStatus.CREATED).body("Punto de accesibilidad creado correctamente");
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al crear el punto de accesibilidad");
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> update(@PathVariable Long id, @RequestBody AccessibilityPoiEntity poi) {
        poi.setId(id);
        int result = accessibilityPoiService.update(poi);
        if (result > 0) {
            return ResponseEntity.ok("Punto de accesibilidad actualizado correctamente");
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        int result = accessibilityPoiService.deleteById(id);
        if (result > 0) {
            return ResponseEntity.ok("Punto de accesibilidad eliminado correctamente");
        }
        return ResponseEntity.notFound().build();
    }
}
