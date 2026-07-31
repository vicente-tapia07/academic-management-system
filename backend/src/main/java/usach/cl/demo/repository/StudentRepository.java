package usach.cl.demo.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import usach.cl.demo.dto.StudentDTO;
import usach.cl.demo.dto.SubjectStatusDTO;
import usach.cl.demo.model.StudentEntity;

import java.util.List;
import java.util.Optional;

@Repository
public class StudentRepository {

    private static final String FIND_ALL =
        "SELECT * FROM student";

    private static final String FIND_BY_ID =
        "SELECT * FROM student WHERE id = ?";

    private static final String INSERT =
        "INSERT INTO student (usuario_id, enrollment_number, first_name, last_name, academic_status) " +
        "VALUES (?, ?, ?, ?, ?)";

    private static final String UPDATE =
        "UPDATE student SET first_name = ?, last_name = ?, academic_status = ? WHERE id = ?";

    private static final String DELETE_BY_ID =
        "DELETE FROM student WHERE id = ?";

    private static final String INSERT_USUARIO =
        "INSERT INTO usuario (rut, email, password_hash, rol) " +
        "VALUES (?, ?, crypt(?, gen_salt('bf', 10)), 'STUDENT') RETURNING id";

    private static final String INSERT_STUDENT_WITH_USER =
        "INSERT INTO student (usuario_id, enrollment_number, first_name, last_name, academic_status) " +
        "VALUES (?, ?, ?, ?, 'ACTIVE')";

    private static final String UPDATE_LOCATION =
        "UPDATE student SET home_location = ST_SetSRID(ST_MakePoint(?, ?), 4326) WHERE id = ?";

    private final JdbcTemplate jdbcTemplate;

    public StudentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void saveWithUsuario(StudentDTO dto) {
        Long usuarioId = jdbcTemplate.queryForObject(
            INSERT_USUARIO, Long.class,
            dto.rut(), dto.email(), dto.password()
        );
        jdbcTemplate.update(
            INSERT_STUDENT_WITH_USER,
            usuarioId, dto.enrollmentNumber(), dto.firstName(), dto.lastName()
        );
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
            e.getUsuarioId(), e.getEnrollmentNumber(),
            e.getFirstName(), e.getLastName(), e.getAcademicStatus());
    }

    public int update(StudentEntity e) {
        return jdbcTemplate.update(UPDATE,
            e.getFirstName(), e.getLastName(), e.getAcademicStatus(), e.getId());
    }

    public int deleteById(Long id) {
        return jdbcTemplate.update(DELETE_BY_ID, id);
    }

    public void updateLocation(Long studentId, Double latitude, Double longitude) {
        int rows = jdbcTemplate.update(UPDATE_LOCATION, longitude, latitude, studentId);
        if (rows == 0) throw new RuntimeException("Student not found: " + studentId);
    }

    public double[] getLocation(Long studentId) {
        String sql = "SELECT ST_Y(home_location::geometry) AS lat, " +
                     "ST_X(home_location::geometry) AS lng " +
                     "FROM student WHERE id = ? AND home_location IS NOT NULL";
        List<double[]> result = jdbcTemplate.query(sql,
            (rs, rowNum) -> new double[]{ rs.getDouble("lat"), rs.getDouble("lng") },
            studentId);
        return result.isEmpty() ? null : result.get(0);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CURRICULUM: conserva cada intento histórico del estudiante y agrega una
    // fila PENDING únicamente para asignaturas que nunca ha inscrito.
    // Así una repetición puede mostrar, por ejemplo, FAILED en 2025-1S y
    // ENROLLED en 2026-1S sin que DISTINCT ON oculte uno de los dos intentos.
    // ─────────────────────────────────────────────────────────────────────────
    private static final String FIND_CURRICULUM =
            "WITH attempts AS ( " +
            "  SELECT sub.id AS subject_id, sub.code AS subject_code, " +
            "    sub.name AS subject_name, sub.credits, " +
            "    CASE " +
            "      WHEN e.status = 'ACTIVE' THEN 'ENROLLED' " +
            "      WHEN g.value >= 4.0 THEN 'APPROVED' " +
            "      WHEN g.value IS NOT NULL THEN 'FAILED' " +
            "      ELSE 'PENDING' " +
            "    END AS status, " +
            "    g.value AS grade, sem.id AS semester_id, " +
            "    sem.year AS semester_year, sem.period AS semester_period " +
            "  FROM enrollment e " +
            "  JOIN section sec ON sec.id = e.section_id " +
            "  JOIN subject sub ON sub.id = sec.subject_id " +
            "  JOIN semester sem ON sem.id = sec.semester_id " +
            "  LEFT JOIN LATERAL ( " +
            "    SELECT grade.value FROM grade " +
            "    WHERE grade.enrollment_id = e.id " +
            "    ORDER BY grade.entry_date DESC, grade.id DESC LIMIT 1 " +
            "  ) g ON true " +
            "  WHERE e.student_id = ? " +
            "    AND e.status IN ('ACTIVE', 'COMPLETED') " +
            "), combined AS ( " +
            "  SELECT * FROM attempts " +
            "  UNION ALL " +
            "  SELECT sub.id, sub.code, sub.name, sub.credits, 'PENDING', " +
            "    NULL::NUMERIC, NULL::BIGINT, NULL::INTEGER, NULL::VARCHAR " +
            "  FROM subject sub " +
            "  WHERE NOT EXISTS ( " +
            "    SELECT 1 FROM attempts a WHERE a.subject_id = sub.id " +
            "  ) " +
            ") " +
            "SELECT subject_id, subject_code, subject_name, credits, status, " +
            "  grade, semester_id, semester_year, semester_period " +
            "FROM combined " +
            "ORDER BY semester_year DESC NULLS LAST, " +
            "  CASE semester_period WHEN '2S' THEN 2 WHEN '1S' THEN 1 ELSE 0 END DESC, " +
            "  subject_code";

    private final RowMapper<SubjectStatusDTO> curriculumMapper = (rs, rowNum) -> {
        SubjectStatusDTO dto = new SubjectStatusDTO();
        dto.setSubjectId(rs.getLong("subject_id"));
        dto.setSubjectCode(rs.getString("subject_code"));
        dto.setSubjectName(rs.getString("subject_name"));
        dto.setCredits(rs.getInt("credits"));
        dto.setStatus(rs.getString("status"));
        double grade = rs.getDouble("grade");
        dto.setGrade(rs.wasNull() ? null : grade);
        long semId = rs.getLong("semester_id");
        dto.setSemesterId(rs.wasNull() ? null : semId);
        int semYear = rs.getInt("semester_year");
        dto.setSemesterYear(rs.wasNull() ? null : semYear);
        dto.setSemesterPeriod(rs.getString("semester_period"));
        return dto;
    };

    public List<SubjectStatusDTO> findCurriculum(Long studentId) {
        return jdbcTemplate.query(FIND_CURRICULUM, curriculumMapper, studentId);
    }
}
