package usach.cl.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import usach.cl.demo.model.ProfessorEntity;
import usach.cl.demo.dto.ProfessorDTO;
import usach.cl.demo.dto.FailureRateDTO;
import usach.cl.demo.model.GradeEntity;
import usach.cl.demo.model.SectionEntity;
import usach.cl.demo.service.ProfessorService;
import usach.cl.demo.service.AuthorizationService;

import java.util.List;

@RestController
@RequestMapping("/api/professors")
public class ProfessorController {

    private final ProfessorService professorService;
    private final AuthorizationService authorizationService;

    public ProfessorController(ProfessorService professorService,
                               AuthorizationService authorizationService) {
        this.professorService = professorService;
        this.authorizationService = authorizationService;
    }


    @GetMapping
    public ResponseEntity<List<ProfessorEntity>> getAll() {
        return ResponseEntity.ok(professorService.getAll());
    }


    @GetMapping("/{id}")
    public ResponseEntity<ProfessorEntity> getById(@PathVariable Long id) {
        return ResponseEntity.ok(professorService.getById(id));
    }


    @PostMapping
    public ResponseEntity<ProfessorEntity> create(@RequestBody ProfessorDTO dto) throws Exception {
        return ResponseEntity.ok(professorService.create(dto));
    }


    @PutMapping("/{id}")
    public ResponseEntity<ProfessorEntity> update(@PathVariable Long id, @RequestBody ProfessorDTO dto) {
        return ResponseEntity.ok(professorService.update(id, dto));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        professorService.delete(id);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/reports")
    public List<FailureRateDTO> getReports(@RequestParam(required = false) String semesterId,
                                           @RequestParam(required = false) String subjectId) {
        return professorService.getFailureReport(semesterId, subjectId);
    }


    @PostMapping("/grade")
    public GradeEntity submitGrade(@RequestBody GradeEntity grade, @RequestParam String professorRut,
                                   Authentication authentication) {
        authorizationService.requireProfessorRut(authentication, professorRut);
        authorizationService.requireProfessorOwnsEnrollment(authentication, grade.getEnrollmentId());
        return professorService.saveGrade(grade, professorRut);
    }

    @GetMapping("/{id}/sections")
    public ResponseEntity<List<SectionEntity>> getSectionsByProfessor(@PathVariable Long id,
                                                                       Authentication authentication) {
        authorizationService.requireProfessorAccess(authentication, id);
        return ResponseEntity.ok(professorService.getSectionsByProfessorId(id));
    }
}
