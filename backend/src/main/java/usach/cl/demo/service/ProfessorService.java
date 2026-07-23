package usach.cl.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import usach.cl.demo.dto.FailureRateDTO;
import usach.cl.demo.model.*;
import usach.cl.demo.repository.*;
import java.util.List;

@Service
public class ProfessorService {

    @Autowired private GradeRepository      gradeRepository;
    @Autowired private FailureRateRepository failureRateRepository;
    @Autowired private AuditRepository       auditRepository;
    @Autowired private ProfessorRepository   professorRepository;
    @Autowired private SectionRepository     sectionRepository;
    @Autowired private JdbcTemplate          jdbcTemplate;
    @Autowired private PasswordEncoder       passwordEncoder;

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
        String hash = passwordEncoder.encode(dto.password());

        // Usar RUT del DTO, o timestamp como fallback si no se envió
        String rut = (dto.rut() != null && !dto.rut().isBlank())
            ? dto.rut()
            : "PROF-" + System.currentTimeMillis();

        // Schema real: id, rut, email, password_hash, rol
        Long usuarioId = jdbcTemplate.queryForObject(
            "INSERT INTO usuario (rut, email, password_hash, rol) " +
            "VALUES (?, ?, ?, 'PROFESSOR') RETURNING id",
            Long.class,
            rut,
            dto.email(),
            hash
        );

        String[] parts    = dto.name().trim().split("\\s+", 2);
        String firstName  = parts[0];
        String lastName   = parts.length > 1 ? parts[1] : "";

        ProfessorEntity professor = new ProfessorEntity();
        professor.setUsuarioId(usuarioId);
        professor.setFirstName(firstName);
        professor.setLastName(lastName);
        professor.setDepartment(dto.department());

        return professorRepository.save(professor);
    }

    public ProfessorEntity update(Long id, usach.cl.demo.dto.ProfessorDTO dto) {
        ProfessorEntity existing = professorRepository.findById(id);
        if (existing == null) throw new RuntimeException("Profesor no encontrado: " + id);

        String[] parts   = dto.name().trim().split("\\s+", 2);
        String firstName = parts[0];
        String lastName  = parts.length > 1 ? parts[1] : "";

        // Actualizar credenciales si se enviaron
        if (dto.email() != null && !dto.email().isBlank()) {
            if (dto.password() != null && !dto.password().isBlank()) {
                String hash = passwordEncoder.encode(dto.password());
                jdbcTemplate.update(
                    "UPDATE usuario SET email = ?, password_hash = ? WHERE id = ?",
                    dto.email(), hash, existing.getUsuarioId()
                );
            } else {
                jdbcTemplate.update(
                    "UPDATE usuario SET email = ? WHERE id = ?",
                    dto.email(), existing.getUsuarioId()
                );
            }
        }

        // updateProfessor(id, department, firstName, lastName)
        professorRepository.updateProfessor(id, dto.department(), firstName, lastName);

        existing.setFirstName(firstName);
        existing.setLastName(lastName);
        existing.setDepartment(dto.department());
        return existing;
    }

    public void delete(Long id) {
        ProfessorEntity existing = professorRepository.findById(id);
        if (existing == null) return;
        Long usuarioId = existing.getUsuarioId();
        jdbcTemplate.update("DELETE FROM professor WHERE id = ?", id);
        if (usuarioId != null) {
            jdbcTemplate.update("DELETE FROM usuario WHERE id = ?", usuarioId);
        }
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
        String newDataJson = "{\"enrollment_id\": " + grade.getEnrollmentId() +
                             ", \"value\": " + grade.getValue() + "}";
        auditRepository.logAudit(
            "grade", "INSERT", professorRut,
            java.time.LocalDateTime.now(), null, newDataJson
        );
        return savedGrade;
    }
}
