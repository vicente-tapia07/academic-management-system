package usach.cl.demo.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import usach.cl.demo.dto.StudentDTO;
import usach.cl.demo.dto.SubjectStatusDTO;
import usach.cl.demo.model.StudentEntity;
import usach.cl.demo.repository.StudentRepository;

import java.util.List;
import java.util.Optional;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    // Inyección recomendada por constructor (sin necesidad de poner @Autowired)
    public StudentService(StudentRepository studentRepository, 
                          JdbcTemplate jdbcTemplate, 
                          PasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    public List<StudentEntity> findAll() {
        return studentRepository.findAll();
    }

    @Transactional
    public void saveWithUsuario(StudentDTO dto) {
        if (dto == null || isBlank(dto.rut()) || isBlank(dto.email()) || isBlank(dto.password()) ||
                isBlank(dto.firstName()) || isBlank(dto.lastName()) || isBlank(dto.enrollmentNumber())) {
            throw new IllegalArgumentException("Todos los datos del estudiante son obligatorios");
        }
        studentRepository.saveWithUsuario(dto);
    }

    public Optional<StudentEntity> findById(Long id) {
        return studentRepository.findById(id);
    }

    public int save(StudentEntity studentEntity) {
        return studentRepository.save(studentEntity);
    }

    public int update(StudentEntity studentEntity) {
        return studentRepository.update(studentEntity);
    }

    @Transactional
    public int deleteById(Long id) {
        Optional<StudentEntity> student = studentRepository.findById(id);
        int deleted = studentRepository.deleteById(id);
        
        // Limpiamos también el usuario vinculado en la BD si existe
        student.ifPresent(s -> {
            if (s.getUsuarioId() != null) {
                jdbcTemplate.update("DELETE FROM usuario WHERE id = ?", s.getUsuarioId());
            }
        });
        
        return deleted;
    }

    public List<SubjectStatusDTO> findCurriculum(Long studentId) {
        return studentRepository.findCurriculum(studentId);
    }

    /**
     * Actualiza la ubicación de residencia del estudiante.
     * Usada cuando el estudiante ingresa su dirección en Mi Perfil.
     */
    public void updateLocation(Long studentId, Double latitude, Double longitude) {
        studentRepository.updateLocation(studentId, latitude, longitude);
    }

    /**
     * Devuelve las coordenadas de home_location del estudiante.
     * Retorna null si no tiene ubicación guardada.
     */
    public double[] getLocation(Long studentId) {
        return studentRepository.getLocation(studentId);
    }

    /**
     * Permite al Administrador actualizar el correo y/o la contraseña
     * del usuario asociado al estudiante.
     */
    public void updateCredentials(Long usuarioId, String newEmail, String newPassword) {
        if (usuarioId == null) return;

        if (newPassword != null && !newPassword.isEmpty()) {
            String hash = passwordEncoder.encode(newPassword);
            jdbcTemplate.update("UPDATE usuario SET email = ?, password_hash = ? WHERE id = ?", newEmail, hash, usuarioId);
        } else if (newEmail != null && !newEmail.isEmpty()) {
            jdbcTemplate.update("UPDATE usuario SET email = ? WHERE id = ?", newEmail, usuarioId);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
