package usach.cl.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import usach.cl.demo.model.RoomEntity;
import usach.cl.demo.service.RoomService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    public ResponseEntity<List<RoomEntity>> findAll(
            @RequestParam(required = false) Long buildingId) {
        if (buildingId != null) {
            return ResponseEntity.ok(roomService.findByBuildingId(buildingId));
        }
        return ResponseEntity.ok(roomService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomEntity> findById(@PathVariable Long id) {
        Optional<RoomEntity> room = roomService.findById(id);
        return room
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<String> save(@RequestBody RoomEntity room) {
        int result = roomService.save(room);
        if (result > 0) {
            return ResponseEntity.status(HttpStatus.CREATED).body("Sala creada correctamente");
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al crear la sala");
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> update(@PathVariable Long id, @RequestBody RoomEntity room) {
        room.setId(id);
        int result = roomService.update(room);
        if (result > 0) {
            return ResponseEntity.ok("Sala actualizada correctamente");
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        int result = roomService.deleteById(id);
        if (result > 0) {
            return ResponseEntity.ok("Sala eliminada correctamente");
        }
        return ResponseEntity.notFound().build();
    }
}