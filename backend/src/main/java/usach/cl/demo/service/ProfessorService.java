package usach.cl.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import usach.cl.demo.dto.FailureRateDTO;
import usach.cl.demo.model.*;
import usach.cl.demo.repository.*;
import java.util.List;

@Service
public class ProfessorService {

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private FailureRateRepository failureRateRepository;

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private SectionRepository sectionRepository;
    
    public List<ProfessorEntity> getAll() {
        return professorRepository.findAll();
    }


    public ProfessorEntity getById(Long id) {
        return professorRepository.findById(id);
    }

    public List<SectionEntity> getSectionsByProfessorId(Long professorId) {
        return sectionRepository.findByProfessorId(professorId);
    }

    public ProfessorEntity create(usach.cl.demo.dto.ProfessorDTO dto) {
        ProfessorEntity professor = new ProfessorEntity();
        professor.setUsuarioId(null);
        String[] names = dto.name().split(" ", 2);
        professor.setFirstName(names.length > 0 ? names[0] : "");
        professor.setLastName(names.length > 1 ? names[1] : "");
        professor.setDepartment(dto.department());
        return professorRepository.save(professor);
    }

    public ProfessorEntity update(Long id, usach.cl.demo.dto.ProfessorDTO dto) {
        ProfessorEntity existing = professorRepository.findByUserId(id);
        String[] names = dto.name().split(" ", 2);
        String firstName = names.length > 0 ? names[0] : "";
        String lastName = names.length > 1 ? names[1] : "";
        professorRepository.updateProfessor(id, dto.department(), firstName, lastName);
        existing.setFirstName(firstName);
        existing.setLastName(lastName);
        existing.setDepartment(dto.department());
        return existing;
    }

    public void delete(Long id) {
        professorRepository.deleteByUserId(id);
    }

    public List<FailureRateDTO> getFailureReport() {
        failureRateRepository.refreshView();
        return failureRateRepository.getFailureRateReport();
    }

    public GradeEntity saveGrade(GradeEntity grade, String professorRut) {

        if (grade.getEntryDate() == null) {
            grade.setEntryDate(java.time.LocalDate.now());
        }

        GradeEntity savedGrade = gradeRepository.save(grade);

        String newDataJson = "{\"enrollment_id\": " + grade.getEnrollmentId() + ", \"value\": " + grade.getValue() + "}";

        auditRepository.logAudit(
                "grade",
                "INSERT",
                professorRut,
                java.time.LocalDateTime.now(),
                null, // oldData (es null porque es un INSERT, no había datos antes)
                newDataJson
        );

        return savedGrade;
    }
}