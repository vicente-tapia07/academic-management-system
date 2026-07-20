package usach.cl.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import usach.cl.demo.dto.NearestRoomRequestDTO;
import usach.cl.demo.dto.NearestRoomResponseDTO;
import usach.cl.demo.service.LocationService;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/location")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @PostMapping("/nearest-room")
    public ResponseEntity<?> nearestRoom(@RequestBody NearestRoomRequestDTO request) {
        Optional<NearestRoomResponseDTO> nearest = locationService.findNearestActiveRoom(
                request.getStudentId(), request.getLat(), request.getLng()
        );

        if (nearest.isPresent()) {
            return ResponseEntity.ok(nearest.get());
        }
        return ResponseEntity.status(404).body(
                Map.of("message", "No se encontró una sala con clase activa en este momento para el estudiante indicado")
        );
    }
}
