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

    private static final String FIND_ALL = "SELECT * FROM student";

    private static final String FIND_BY_ID = "SELECT * FROM student WHERE id = ?";

    private static final String INSERT = "INSERT INTO student (usuario_id, enrollment_number, first_name, last_name, academic_status) " + "VALUES (?, ?, ?, ?, ?)";

    private static final String UPDATE = "UPDATE student SET first_name = ?, last_name = ?, academic_status = ? " + "WHERE id = ?";

    private static final String DELETE_BY_ID = "DELETE FROM student WHERE id = ?";

    private final JdbcTemplate jdbcTemplate;

    public StudentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<StudentEntity> studentMapper = (rs, rowNum) -> {
        StudentEntity e = new StudentEntity();
        e.setId(rs.getLong("id"));
        e.setUsuarioId(rs.getLong("usuario_id"));
        e.setEnrollmentNumber(rs.getString("enrollment_number"));
        e.setFirstName(rs.getString("first_name"));
        e.setLastName(rs.getString("last_name"));
        e.setAcademicStatus(rs.getString("academic_status"));
        return e;
    };

    public List<StudentEntity> findAll() {
        return jdbcTemplate.query(FIND_ALL, studentMapper);
    }

    public Optional<StudentEntity> findById(Long id) {
        List<StudentEntity> result = jdbcTemplate.query(FIND_BY_ID, studentMapper, id);
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

    // Mapper para convertir cada fila en un SubjectStatusDTO
    private final RowMapper<SubjectStatusDTO> curriculumMapper = (rs, rowNum) -> {
        SubjectStatusDTO dto = new SubjectStatusDTO();
        dto.setSubjectId(rs.getLong("subject_id"));
        dto.setSubjectCode(rs.getString("subject_code"));
        dto.setSubjectName(rs.getString("subject_name"));
        dto.setCredits(rs.getInt("credits"));
        dto.setStatus(rs.getString("status"));
        // grade puede ser null si es PENDING o ENROLLED
        double grade = rs.getDouble("grade");
        dto.setGrade(rs.wasNull() ? null : grade);
        return dto;
    };

    // Retorna la malla curricular de un estudiante
    public List<SubjectStatusDTO> findCurriculum(Long studentId) {
        return jdbcTemplate.query(FIND_CURRICULUM, curriculumMapper, studentId);
    }

}
