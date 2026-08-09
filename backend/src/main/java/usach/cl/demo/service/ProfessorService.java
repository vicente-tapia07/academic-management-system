package usach.cl.demo.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import usach.cl.demo.dto.FailureRateDTO;
import usach.cl.demo.dto.ProfessorDTO;
import usach.cl.demo.model.GradeEntity;
import usach.cl.demo.model.ProfessorEntity;
import usach.cl.demo.model.SectionEntity;
import usach.cl.demo.repository.MongoGradeRepository;
import usach.cl.demo.repository.MongoProfessorRepository;
import usach.cl.demo.repository.MongoSectionRepository;

import java.time.LocalDate;
import java.util.List;

@Service
public class ProfessorService {

    private final MongoProfessorRepository professorRepository;
    private final MongoSectionRepository sectionRepository;
    private final MongoGradeRepository gradeRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfessorService(MongoProfessorRepository professorRepository,
                            MongoSectionRepository sectionRepository,
                            MongoGradeRepository gradeRepository,
                            PasswordEncoder passwordEncoder) {
        this.professorRepository = professorRepository;
        this.sectionRepository = sectionRepository;
        this.gradeRepository = gradeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<ProfessorEntity> getAll() {
        return professorRepository.findAll();
    }

    public ProfessorEntity getById(Long id) {
        return professorRepository.findById(id);
    }

    public List<SectionEntity> getSectionsByProfessorId(Long professorId) {
        return sectionRepository.findByProfessorId(professorId);
    }

    public ProfessorEntity create(ProfessorDTO dto) {
        validateProfessor(dto, true);
        return professorRepository.saveWithUsuario(dto, passwordEncoder.encode(dto.password()));
    }

    public ProfessorEntity update(Long id, ProfessorDTO dto) {
        validateProfessor(dto, false);
        ProfessorEntity existing = professorRepository.findById(id);
        if (existing == null) throw new RuntimeException("Profesor no encontrado: " + id);

        String[] parts = dto.name().trim().split("\\s+", 2);
        String firstName = parts[0];
        String lastName = parts.length > 1 ? parts[1] : "";

        if (dto.email() != null && !dto.email().isBlank()) {
            String passwordHash = (dto.password() != null && !dto.password().isBlank())
                    ? passwordEncoder.encode(dto.password()) : null;
            professorRepository.updateCredentials(id, dto.email(), null, passwordHash);
        }

        professorRepository.updateProfessor(id, dto.department(), firstName, lastName);

        existing.setFirstName(firstName);
        existing.setLastName(lastName);
        existing.setDepartment(dto.department());
        return existing;
    }

    public void delete(Long id) {
        ProfessorEntity existing = professorRepository.findById(id);
        if (existing == null) return;
        professorRepository.deleteByUserId(id);
    }

    public List<FailureRateDTO> getFailureReport() {
        return getFailureReport(null, null);
    }

    public List<FailureRateDTO> getFailureReport(String semesterId, String subjectId) {
        List<FailureRateDTO> report = gradeRepository.getFailureRateReport();
        if (semesterId != null && !semesterId.isBlank()) {
            report = report.stream()
                    .filter(dto -> semesterId.equals(dto.getSemesterId()))
                    .toList();
        }
        if (subjectId != null && !subjectId.isBlank()) {
            report = report.stream()
                    .filter(dto -> subjectId.equals(dto.getSubjectId()))
                    .toList();
        }
        return report;
    }

    public GradeEntity saveGrade(GradeEntity grade, String professorRut) {
        if (grade == null || grade.getEnrollmentId() == null || grade.getValue() == null) {
            throw new IllegalArgumentException("enrollmentId y value son obligatorios");
        }
        if (grade.getValue() < 1.0 || grade.getValue() > 7.0) {
            throw new IllegalArgumentException("La nota debe estar entre 1.0 y 7.0");
        }
        if (grade.getEntryDate() == null) {
            grade.setEntryDate(LocalDate.now());
        }
        return gradeRepository.save(grade, professorRut);
    }

    private void validateProfessor(ProfessorDTO dto, boolean passwordRequired) {
        if (dto == null || dto.name() == null || dto.name().isBlank() ||
                dto.email() == null || dto.email().isBlank() ||
                dto.department() == null || dto.department().isBlank() ||
                (passwordRequired && (dto.password() == null || dto.password().isBlank()))) {
            throw new IllegalArgumentException("Nombre, email, departamento y contraseña son obligatorios");
        }
    }
}
