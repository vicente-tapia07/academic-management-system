package usach.cl.demo.service;

import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import usach.cl.demo.entity.Professor;
import usach.cl.demo.entity.Student;
import usach.cl.demo.entity.User;
import usach.cl.demo.model.*;
import usach.cl.demo.repository.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProfessorService {

    private final ProfessorRepository professorRepository;
    private final UserService userService;
    private final GradeRepository gradeRepository;
    private final FailureRateRepository failureRateRepository;
    private final AuditRepository auditRepository;

    public ProfessorService(ProfessorRepository professorRepository,
                            UserService userService,
                            GradeRepository gradeRepository,
                            FailureRateRepository failureRateRepository,
                            AuditRepository auditRepository) {
        this.professorRepository = professorRepository;
        this.userService = userService;
        this.gradeRepository = gradeRepository;
        this.failureRateRepository = failureRateRepository;
        this.auditRepository = auditRepository;
    }

    @Transactional
    public Professor create(@Nonnull ProfessorDto dto) throws Exception {
        User user = userService.create(
                new UserDto(dto.name(), dto.email(), dto.password(), Role.PROFESSOR)
        );
        Professor professor = new Professor(user, dto.department());
        return professorRepository.save(professor);
    }

    public Professor getById(int userId) {
        return professorRepository.findByUserId(userId);
    }

    public List<Professor> getAll() {
        return professorRepository.findAll();
    }

    @Transactional
    public Professor update(int userId, @Nonnull ProfessorDto dto) {
        userService.updateUser(userId, dto.name(), dto.email());
        professorRepository.updateProfessor(userId, dto.department());
        return getById(userId);
    }

    @Transactional
    public void delete(int userId) {
        professorRepository.deleteByUserId(userId);
        userService.deleteUser(userId);
    }

    public List<FailureRateDTO> getFailureReport() {
        failureRateRepository.refreshView();
        return failureRateRepository.getFailureRateReport();
    }

    public GradeEntity saveGrade(GradeEntity grade, String professorRut) {
        if (grade.getEntryDate() == null) {
            grade.setEntryDate(LocalDate.now());
        }

        GradeEntity savedGrade = gradeRepository.save(grade);

        String newDataJson = "{\"enrollment_id\": " + grade.getEnrollmentId() +
                ", \"value\": " + grade.getValue() + "}";

        auditRepository.logAudit(
                "grade",
                "INSERT",
                professorRut,
                LocalDateTime.now(),
                null,
                newDataJson
        );

        return savedGrade;
    }
}