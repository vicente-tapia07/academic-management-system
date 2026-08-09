package usach.cl.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import usach.cl.demo.dto.RoomDTO;
import usach.cl.demo.model.SectionRoom;
import usach.cl.demo.service.SectionService;

import java.util.List;

/**
 * Salas derivadas de las secciones existentes (sala embebida).
 */
@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final SectionService sectionService;

    public RoomController(SectionService sectionService) {
        this.sectionService = sectionService;
    }

    @GetMapping
    public ResponseEntity<List<RoomDTO>> getAll() {
        return ResponseEntity.ok(sectionService.findDistinctRooms().stream()
                .map(RoomController::toDto)
                .toList());
    }

    @GetMapping("/{code}")
    public ResponseEntity<RoomDTO> getById(@PathVariable String code) {
        return sectionService.findDistinctRooms().stream()
                .filter(room -> room.getCode().equals(code))
                .findFirst()
                .map(room -> ResponseEntity.ok(toDto(room)))
                .orElse(ResponseEntity.notFound().build());
    }

    private static RoomDTO toDto(SectionRoom room) {
        return new RoomDTO(room.getCode(), room.getCode(), room.getName(), room.getBuilding());
    }
}
