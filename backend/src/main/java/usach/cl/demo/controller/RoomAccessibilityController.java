package usach.cl.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import usach.cl.demo.dto.AccessibleRoomDTO;
import usach.cl.demo.service.AccessibilityPoiService;

import java.util.List;

// Endpoint de Accesibilidad (I2). Ruta compartida con el recurso /api/rooms de I1,
// pero vive en su propia clase para no tocar RoomController.java (dueño: I1).
@RestController
@RequestMapping("/api/rooms")
public class RoomAccessibilityController {

    private final AccessibilityPoiService accessibilityPoiService;

    public RoomAccessibilityController(AccessibilityPoiService accessibilityPoiService) {
        this.accessibilityPoiService = accessibilityPoiService;
    }

    @GetMapping("/accessible")
    public ResponseEntity<List<AccessibleRoomDTO>> findAccessibleRooms(
            @RequestParam(required = false) Long buildingId) {
        return ResponseEntity.ok(accessibilityPoiService.findAccessibleRooms(buildingId));
    }
}