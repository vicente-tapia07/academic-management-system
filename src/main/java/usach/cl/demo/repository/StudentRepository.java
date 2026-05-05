package usach.cl.demo.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import usach.cl.demo.dto.SubjectStatusDTO;
import usach.cl.demo.model.StudentEntity;

import java.util.List;
import java.util.Optional;

@Repository
public class StudentRepository {

    private static final String FIND_ALL = "SELECT * FROM estudiante";

    private static final String FIND_BY_ID = "SELECT * FROM estudiante WHERE id = ?";

    private static final String INSERT = "INSERT INTO estudiante (usuario_id, matricula, nombre, apellido, estado_academico) " +
            "VALUES (?, ?, ?, ?, ?)";

    private static final String UPDATE = "UPDATE estudiante SET nombre = ?, apellido = ?, estado_academico = ? " +
            "WHERE id = ?";

    private static final String DELETE_BY_ID = "DELETE FROM estudiante WHERE id = ?";

    private final JdbcTemplate jdbcTemplate;

    public StudentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<StudentEntity> estudianteMapper = (rs, rowNum) -> {
        StudentEntity e = new StudentEntity();
        e.setId(rs.getLong("id"));
        e.setUsuarioId(rs.getLong("usuario_id"));
        e.setEnrollmentNumber(rs.getString("matricula"));
        e.setFirstName(rs.getString("nombre"));
        e.setLastName(rs.getString("apellido"));
        e.setAcademicStatus(rs.getString("estado_academico"));
        return e;
    };

    public List<StudentEntity> findAll() {
        return jdbcTemplate.query(FIND_ALL, estudianteMapper);
    }

    public Optional<StudentEntity> findById(Long id) {
        List<StudentEntity> result = jdbcTemplate.query(FIND_BY_ID, estudianteMapper, id);
        return result.stream().findFirst();
    }

    public int save(StudentEntity e) {
        return jdbcTemplate.update(INSERT,
                e.getUsuarioId(),
                e.getEnrollmentNumber(),
                e.getFirstName(),
                e.getLastName(),
                e.getAcademicStatus());
    }

    public int update(StudentEntity e) {
        return jdbcTemplate.update(UPDATE,
                e.getFirstName(),
                e.getLastName(),
                e.getAcademicStatus(),
                e.getId());
    }

    public int deleteById(Long id) {
        return jdbcTemplate.update(DELETE_BY_ID, id);
    }

    private static final String FIND_CURRICULUM =
            "SELECT DISTINCT ON (sub.id) " +
                    "sub.id AS subject_id, " +
                    "sub.code AS subject_code, " +
                    "sub.name AS subject_name, " +
                    "sub.credits, " +
                    "CASE " +
                    "WHEN e.id IS NULL THEN 'PENDING' " +
                    "WHEN g.value IS NULL THEN 'ENROLLED' " +
                    "WHEN g.value >= 4.0 THEN 'APPROVED' " +
                    "ELSE 'FAILED' " +
                    "END AS status, " +
                    "g.value AS grade " +
                    "FROM subject sub " +
                    "LEFT JOIN section sec ON sec.subject_id = sub.id " +
                    "LEFT JOIN enrollment e ON e.section_id = sec.id AND e.student_id = ? " +
                    "LEFT JOIN grade g ON g.enrollment_id = e.id " +
                    "ORDER BY sub.id, g.value DESC NULLS LAST";

    private final RowMapper<SubjectStatusDTO> curriculumMapper = (rs, rowNum) -> {
        SubjectStatusDTO dto = new SubjectStatusDTO();
        dto.setSubjectId(rs.getLong("subject_id"));
        dto.setSubjectCode(rs.getString("subject_code"));
        dto.setSubjectName(rs.getString("subject_name"));
        dto.setCredits(rs.getInt("credits"));
        dto.setStatus(rs.getString("status"));
        double grade = rs.getDouble("grade");
        dto.setGrade(rs.wasNull() ? null : grade);
        return dto;
    };

    public List<SubjectStatusDTO> findCurriculum(Long studentId) {
        return jdbcTemplate.query(FIND_CURRICULUM, curriculumMapper, studentId);
    }
}