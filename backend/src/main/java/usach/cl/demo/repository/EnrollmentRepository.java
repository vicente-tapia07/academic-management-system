package usach.cl.demo.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import usach.cl.demo.model.EnrollmentEntity;

import java.util.List;
import java.util.Optional;

@Repository
public class EnrollmentRepository {

    private static final String FIND_ALL =
            "SELECT * FROM enrollment";

    private static final String FIND_BY_ID =
            "SELECT * FROM enrollment WHERE id = ?";

    private static final String FIND_BY_STUDENT_ID =
            "SELECT * FROM enrollment WHERE student_id = ?";

    private static final String INSERT =
            "INSERT INTO enrollment (student_id, section_id, enrollment_date, status) " +
                    "VALUES (?, ?, ?, ?)";

    private static final String UPDATE_STATUS =
            "UPDATE enrollment SET status = ? WHERE id = ?";

    private static final String DELETE_BY_ID =
            "DELETE FROM enrollment WHERE id = ?";

    private static final String CALL_ENROLL_STUDENT =
            "CALL sp_enroll_student(?, ?)";

    private static final String FIND_BY_SECTION_ID =
            "SELECT * FROM enrollment WHERE section_id = ?";

    private static final String GET_SECTION_ID_BY_ENROLLMENT =
        "SELECT section_id FROM enrollment WHERE id = ?";

    private static final String RESTORE_SEAT =
        "UPDATE section SET available_seats = available_seats + 1 WHERE id = ?";

    private final JdbcTemplate jdbcTemplate;

    public List<EnrollmentEntity> findBySectionId(Long sectionId) {
        return jdbcTemplate.query(FIND_BY_SECTION_ID, enrollmentMapper, sectionId);
    }

    public EnrollmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<EnrollmentEntity> enrollmentMapper = (rs, rowNum) -> {
        EnrollmentEntity e = new EnrollmentEntity();
        e.setId(rs.getLong("id"));
        e.setStudentId(rs.getLong("student_id"));
        e.setSectionId(rs.getLong("section_id"));
        e.setEnrollmentDate(rs.getDate("enrollment_date").toLocalDate());
        e.setStatus(rs.getString("status"));
        return e;
    };

    public List<EnrollmentEntity> findAll() {
        return jdbcTemplate.query(FIND_ALL, enrollmentMapper);
    }

    public Optional<EnrollmentEntity> findById(Long id) {
        List<EnrollmentEntity> result = jdbcTemplate.query(FIND_BY_ID, enrollmentMapper, id);
        return result.stream().findFirst();
    }

    public List<EnrollmentEntity> findByStudentId(Long studentId) {
        return jdbcTemplate.query(FIND_BY_STUDENT_ID, enrollmentMapper, studentId);
    }

    public int save(EnrollmentEntity enrollment) {
        return jdbcTemplate.update(INSERT,
                enrollment.getStudentId(),
                enrollment.getSectionId(),
                enrollment.getEnrollmentDate(),
                enrollment.getStatus()
        );
    }

    public int updateStatus(Long id, String status) {
        return jdbcTemplate.update(UPDATE_STATUS, status, id);
    }

    public int deleteById(Long id) {
        return jdbcTemplate.update(DELETE_BY_ID, id);
    }

    public void enrollStudent(Long studentId, Long sectionId) {
        jdbcTemplate.update(CALL_ENROLL_STUDENT, studentId, sectionId);
    }

    public Long getSectionIdByEnrollmentId(Long enrollmentId) {
        List<Long> result = jdbcTemplate.query(
            GET_SECTION_ID_BY_ENROLLMENT,
            (rs, rn) -> rs.getLong("section_id"),
            enrollmentId
        );
        return result.isEmpty() ? null : result.get(0);
    }

    public int restoreSeat(Long sectionId) {
        return jdbcTemplate.update(RESTORE_SEAT, sectionId);
    }
}