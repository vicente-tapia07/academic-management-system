package usach.cl.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import usach.cl.demo.model.FailureRateDTO;
import usach.cl.demo.service.ProfessorService;
import java.util.List;
import usach.cl.demo.model.GradeEntity;

@RestController
@RequestMapping("/api/professor")
public class ProfessorController {

    @Autowired
    private ProfessorService professorService;

    @GetMapping("/reports")
    public List<FailureRateDTO> getReports() {
        return professorService.getFailureReport();
    }

    @PostMapping("/grade")
    public GradeEntity submitGrade(@RequestBody GradeEntity grade, @RequestParam String professorRut) {
        // En una app real, el RUT del profesor se saca del Token JWT (Keycloak)
        // Por ahora, lo pasamos por la URL para probar que la Auditoría funciona.
        return professorService.saveGrade(grade, professorRut);
    }
}