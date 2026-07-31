package usach.cl.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import usach.cl.demo.dto.StudentDTO;
import usach.cl.demo.dto.SubjectStatusDTO;
import usach.cl.demo.model.StudentEntity;
import usach.cl.demo.service.StudentService;
import usach.cl.demo.service.AuthorizationService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;
    private final AuthorizationService authorizationService;

    public StudentController(StudentService studentService, AuthorizationService authorizationService) {
        this.studentService = studentService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public ResponseEntity<List<StudentEntity>> getAll() {
        return ResponseEntity.ok(studentService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentEntity> getById(@PathVariable Long id) {
        Optional<StudentEntity> student = studentService.findById(id);
        return student.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/curriculum")
    public ResponseEntity<List<SubjectStatusDTO>> getCurriculum(@PathVariable Long id,
                                                                 Authentication authentication) {
        authorizationService.requireStudentAccess(authentication, id);
        List<SubjectStatusDTO> curriculum = studentService.findCurriculum(id);
        if (curriculum.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(curriculum);
    }

    @PostMapping
    public ResponseEntity<String> create(@RequestBody StudentDTO dto) {
        try {
            studentService.saveWithUsuario(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body("Estudiante creado correctamente");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> update(@PathVariable Long id, @RequestBody StudentEntity studentEntity) {
        studentEntity.setId(id);
        int result = studentService.update(studentEntity);
        if (result > 0) return ResponseEntity.ok("Student updated successfully");
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        int result = studentService.deleteById(id);
        if (result > 0) return ResponseEntity.ok("Student deleted successfully");
        return ResponseEntity.notFound().build();
    }

    /**
     * GET /api/students/{id}/location
     *
     * Devuelve las coordenadas de home_location del estudiante.
     * Respuesta: { "latitude": -33.4489, "longitude": -70.6693 }
     * Si no tiene ubicación guardada, devuelve 404.
     */
    @GetMapping("/{id}/location")
    public ResponseEntity<Map<String, Double>> getLocation(@PathVariable Long id,
                                                            Authentication authentication) {
        authorizationService.requireStudentAccess(authentication, id);
        double[] coords = studentService.getLocation(id);
        if (coords == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("latitude", coords[0], "longitude", coords[1]));
    }

    /**
     * PATCH /api/students/{id}/location
     *
     * Actualiza la ubicación de residencia del estudiante.
     * Body: { "latitude": -33.4489, "longitude": -70.6693 }
     *
     * Guarda como GEOMETRY(POINT, 4326) en la columna home_location.
     * El estudiante obtiene estas coordenadas mediante geocodificación
     * en el frontend (Nominatim/OpenStreetMap).
     */
    @PatchMapping("/{id}/location")
    public ResponseEntity<String> updateLocation(
            @PathVariable Long id,
            @RequestBody Map<String, Double> body,
            Authentication authentication) {
        authorizationService.requireStudentAccess(authentication, id);
        Double lat = body.get("latitude");
        Double lng = body.get("longitude");

        if (lat == null || lng == null) {
            return ResponseEntity.badRequest().body("Se requieren latitude y longitude");
        }
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            return ResponseEntity.badRequest().body("Coordenadas fuera de rango");
        }

        try {
            studentService.updateLocation(id, lat, lng);
            return ResponseEntity.ok("Ubicación actualizada correctamente");
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
