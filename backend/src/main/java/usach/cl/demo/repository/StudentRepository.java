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
    // CURRICULUM: muestra todas las asignaturas de la carrera con su estado.
    //
    // La clave del fix: el JOIN de sección ya filtra por estudiante,
    // así que el DISTINCT ON siempre ve la fila con inscripción cuando existe.
    // ORDER BY prioriza filas CON enrollment (e.id NOT NULL) para que el
    // DISTINCT ON las elija sobre filas sin inscripción de otras secciones.
    // ─────────────────────────────────────────────────────────────────────────
    private static final String FIND_CURRICULUM =
        "SELECT DISTINCT ON (sub.id) " +
        "  sub.id          AS subject_id, " +
        "  sub.code        AS subject_code, " +
        "  sub.name        AS subject_name, " +
        "  sub.credits, " +
        "  CASE " +
        "    WHEN e.id IS NULL     THEN 'PENDING' " +
        "    WHEN g.value IS NULL  THEN 'ENROLLED' " +
        "    WHEN g.value >= 4.0   THEN 'APPROVED' " +
        "    ELSE                       'FAILED' " +
        "  END AS status, " +
        "  g.value AS grade " +
        "FROM subject sub " +
        "LEFT JOIN career c ON sub.career_id = c.id " +
        // Solo secciones donde ESTE estudiante tiene inscripción activa o completada
        "LEFT JOIN section sec " +
        "  ON sec.subject_id = sub.id " +
        "LEFT JOIN enrollment e " +
        "  ON e.section_id = sec.id " +
        "  AND e.student_id = ? " +
        "  AND e.status IN ('ACTIVE', 'COMPLETED') " +
        "LEFT JOIN grade g ON g.enrollment_id = e.id " +
        "ORDER BY sub.id, " +
        // Filas con enrollment primero → DISTINCT ON elige esas
        "  CASE WHEN e.id IS NOT NULL THEN 0 ELSE 1 END, " +
        "  g.value DESC NULLS LAST";

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
