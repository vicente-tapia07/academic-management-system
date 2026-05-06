package usach.cl.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import usach.cl.demo.dto.FailureRateDTO;
import usach.cl.demo.model.GradeEntity;
import usach.cl.demo.model.ProfessorEntity;
import usach.cl.demo.repository.ProfessorRepository;
import usach.cl.demo.service.ProfessorService;
import java.util.List;

@RestController
@RequestMapping("/api/professor")
public class ProfessorController {

    @Autowired
    private ProfessorService professorService;

    @Autowired
    private ProfessorRepository professorRepository;

    @GetMapping("/reports")
    public List<FailureRateDTO> getReports() {
        return professorService.getFailureReport();
    }

    @PostMapping("/grade")
    public GradeEntity submitGrade(@RequestBody GradeEntity grade,
                                   @RequestParam String professorRut) {
        return professorService.saveGrade(grade, professorRut);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProfessorEntity> getProfessorById(@PathVariable Long id) {
        return professorRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}