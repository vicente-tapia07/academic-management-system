package usach.cl.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import usach.cl.demo.dto.NearestRoomRequestDTO;
import usach.cl.demo.dto.NearestRoomResponseDTO;
import usach.cl.demo.service.LocationService;
import usach.cl.demo.service.AuthorizationService;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/location")
public class LocationController {

    private final LocationService locationService;
    private final AuthorizationService authorizationService;

    public LocationController(LocationService locationService,
                              AuthorizationService authorizationService) {
        this.locationService = locationService;
        this.authorizationService = authorizationService;
    }

    @PostMapping("/nearest-room")
    public ResponseEntity<?> nearestRoom(@RequestBody NearestRoomRequestDTO request,
                                         Authentication authentication) {
        if (request.getStudentId() == null || request.getLat() == null || request.getLng() == null) {
            throw new IllegalArgumentException("studentId, lat y lng son obligatorios");
        }
        if (request.getLat() < -90 || request.getLat() > 90 ||
                request.getLng() < -180 || request.getLng() > 180) {
            throw new IllegalArgumentException("Coordenadas fuera de rango");
        }
        authorizationService.requireStudentAccess(authentication, request.getStudentId());
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
