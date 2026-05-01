package usach.cl.demo.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import usach.cl.demo.model.EnrollmentEntity;

import java.util.List;
import java.util.Optional;

@Repository
public class EnrollmentRepository {

    // Consultas SQL
    private static final String FIND_ALL =
            "SELECT * FROM inscripcion";

    private static final String FIND_BY_ID =
            "SELECT * FROM inscripcion WHERE id = ?";

    private static final String FIND_BY_STUDENT_ID =
            "SELECT * FROM inscripcion WHERE student_id = ?";

    private static final String INSERT =
            "INSERT INTO inscripcion (student_id, seccion_id, enrollment_date, status) " +
                    "VALUES (?, ?, ?, ?)";

    private static final String UPDATE_STATUS =
            "UPDATE inscripcion SET status = ? WHERE id = ?";

    private static final String DELETE_BY_ID =
            "DELETE FROM inscripcion WHERE id = ?";

    private final JdbcTemplate jdbcTemplate;

    // Spring inyecta el JdbcTemplate automáticamente
    public EnrollmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Convierte una fila SQL en un objeto EnrollmentEntity
    private final RowMapper<EnrollmentEntity> enrollmentMapper = (rs, rowNum) -> {
        EnrollmentEntity e = new EnrollmentEntity();
        e.setId(rs.getLong("id"));
        e.setStudentId(rs.getLong("student_id"));
        e.setSectionId(rs.getLong("seccion_id"));
        e.setEnrollmentDate(rs.getDate("enrollment_date").toLocalDate());
        e.setStatus(rs.getString("status"));
        return e;
    };

    // Retorna todas las inscripciones
    public List<EnrollmentEntity> findAll() {
        return jdbcTemplate.query(FIND_ALL, enrollmentMapper);
    }

    // Retorna una inscripción por su ID
    public Optional<EnrollmentEntity> findById(Long id) {
        List<EnrollmentEntity> result = jdbcTemplate.query(FIND_BY_ID, enrollmentMapper, id);
        return result.stream().findFirst();
    }

    // Retorna todas las inscripciones de un estudiante
    public List<EnrollmentEntity> findByStudentId(Long studentId) {
        return jdbcTemplate.query(FIND_BY_STUDENT_ID, enrollmentMapper, studentId);
    }

    // Crea una nueva inscripción
    public int save(EnrollmentEntity enrollment) {
        return jdbcTemplate.update(INSERT,
                enrollment.getStudentId(),
                enrollment.getSectionId(),
                enrollment.getEnrollmentDate(),
                enrollment.getStatus()
        );
    }

    // Actualiza el estado de una inscripción
    public int updateStatus(Long id, String status) {
        return jdbcTemplate.update(UPDATE_STATUS, status, id);
    }

    // Elimina una inscripción por su ID
    public int deleteById(Long id) {
        return jdbcTemplate.update(DELETE_BY_ID, id);
    }
}