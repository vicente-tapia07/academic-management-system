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

    @Autowired private GradeRepository gradeRepository;
    @Autowired private FailureRateRepository failureRateRepository;
    @Autowired private AuditRepository auditRepository;
    @Autowired private ProfessorRepository professorRepository;
    @Autowired private SectionRepository sectionRepository;
    
    // Inyecciones para gestionar credenciales
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PasswordEncoder passwordEncoder;

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
        // 1. Crear credenciales en la tabla usuario y obtener el ID generado
        String hash = passwordEncoder.encode(dto.password());
        String sqlUser = "INSERT INTO usuario (nombre_completo, email, password_hash, rol, activo, creado_en) " +
                         "VALUES (?, ?, ?, 'ROLE_PROFESSOR', true, NOW()) RETURNING id";
        Long usuarioId = jdbcTemplate.queryForObject(sqlUser, Long.class, dto.name(), dto.email(), hash);

        // 2. Crear el registro del profesor ligado al usuario
        ProfessorEntity professor = new ProfessorEntity();
        professor.setUsuarioId(usuarioId);
        String[] names = dto.name().split(" ", 2);
        professor.setFirstName(names.length > 0 ? names[0] : "");
        professor.setLastName(names.length > 1 ? names[1] : "");
        professor.setDepartment(dto.department());
        
        return professorRepository.save(professor);
    }

    public ProfessorEntity update(Long id, usach.cl.demo.dto.ProfessorDTO dto) {
        ProfessorEntity existing = professorRepository.findById(id); 
        
        // 1. Actualizar credenciales si el Admin envía un nuevo correo o contraseña
        if (dto.email() != null && !dto.email().isEmpty()) {
            if (dto.password() != null && !dto.password().isEmpty()) {
                String hash = passwordEncoder.encode(dto.password());
                jdbcTemplate.update("UPDATE usuario SET email = ?, password_hash = ?, nombre_completo = ? WHERE id = ?", 
                                    dto.email(), hash, dto.name(), existing.getUsuarioId());
            } else {
                jdbcTemplate.update("UPDATE usuario SET email = ?, nombre_completo = ? WHERE id = ?", 
                                    dto.email(), dto.name(), existing.getUsuarioId());
            }
        }

        // 2. Actualizar los datos públicos del profesor
        String[] names = dto.name().split(" ", 2);
        String firstName = names.length > 0 ? names[0] : "";
        String lastName = names.length > 1 ? names[1] : "";
        
        existing.setFirstName(firstName);
        existing.setLastName(lastName);
        existing.setDepartment(dto.department());
        
        professorRepository.updateProfessor(id, dto.department(), firstName, lastName);
        return existing;
    }

    public void delete(Long id) {
        ProfessorEntity existing = professorRepository.findById(id);
        if (existing != null) {
            // Usamos el método deleteByUserId que sí existe en tu repositorio original
            professorRepository.deleteByUserId(existing.getUsuarioId());
            
            // Y luego borramos el usuario vinculado
            jdbcTemplate.update("DELETE FROM usuario WHERE id = ?", existing.getUsuarioId());
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
        String newDataJson = "{\"enrollment_id\": " + grade.getEnrollmentId() + ", \"value\": " + grade.getValue() + "}";
        auditRepository.logAudit("grade", "INSERT", professorRut, java.time.LocalDateTime.now(), null, newDataJson);
        return savedGrade;
    }
}