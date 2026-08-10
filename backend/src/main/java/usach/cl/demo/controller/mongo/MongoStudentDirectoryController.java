package usach.cl.demo.controller.mongo;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import usach.cl.demo.dto.mongo.StudentDirectoryItem;
import usach.cl.demo.service.mongo.StudentDirectoryService;

import java.util.List;

/**
 * Directorio de estudiantes de MongoDB (Integrante 4 · Frontend 2).
 *
 * Se expone bajo /api/mongo/directory y NO bajo /api/mongo/students para no
 * quedar atrapado por la regla de SecurityConfig
 *
 *     .requestMatchers(HttpMethod.GET, "/api/mongo/students/**")
 *         .hasAnyRole("ADMIN", "STUDENT")
 *
 * que dejaría fuera al rol PROFESSOR, el cual sí debe poder emitir
 * certificados de notas.
 *
 * Clase independiente: no modifica MongoEnrollmentController (Backend 1) ni
 * MongoReportController (Backend 2).
 */
@RestController
@RequestMapping("/api/mongo/directory")
public class MongoStudentDirectoryController {

    private final StudentDirectoryService studentDirectoryService;

    public MongoStudentDirectoryController(StudentDirectoryService studentDirectoryService) {
        this.studentDirectoryService = studentDirectoryService;
    }

    /**
     * GET /api/mongo/directory/students
     *
     * Devuelve el listado de estudiantes con su ObjectId, para alimentar los
     * selectores del frontend.
     */
    @GetMapping("/students")
    public ResponseEntity<List<StudentDirectoryItem>> listStudents() {
        return ResponseEntity.ok(studentDirectoryService.listStudents());
    }
}
