package usach.cl.demo.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import usach.cl.demo.dto.NearbySectionResponse;
import usach.cl.demo.model.EnrollmentEntity;

import java.util.List;
import java.util.Optional;

@Repository
public class EnrollmentRepository {

    private static final String FIND_ALL =
            "SELECT * FROM enrollment ORDER BY enrollment_date DESC, id DESC";

    private static final String FIND_BY_ID =
            "SELECT * FROM enrollment WHERE id = ?";

    private static final String FIND_BY_STUDENT_ID =
            "SELECT * FROM enrollment WHERE student_id = ? " +
            "ORDER BY enrollment_date DESC, id DESC";

    private static final String INSERT =
            "INSERT INTO enrollment (student_id, section_id, enrollment_date, status) " +
                    "VALUES (?, ?, ?, ?)";

    private static final String UPDATE_STATUS =
            "UPDATE enrollment SET status = ? WHERE id = ?";

    private static final String CALL_ENROLL_STUDENT =
            "CALL sp_enroll_student(?, ?)";

    private static final String FIND_BY_SECTION_ID =
            "SELECT * FROM enrollment WHERE section_id = ?";

    private static final String CANCEL_AND_RESTORE_SEAT = """
        WITH cancelled AS (
            UPDATE enrollment
            SET status = 'CANCELLED'
            WHERE id = ? AND status = 'ACTIVE'
            RETURNING section_id
        )
        UPDATE section sec
        SET available_seats = LEAST(sec.total_seats, sec.available_seats + 1)
        FROM cancelled
        WHERE sec.id = cancelled.section_id
        RETURNING sec.id
        """;

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

    public boolean hasGrade(Long enrollmentId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM grade WHERE enrollment_id = ?", Integer.class, enrollmentId);
        return count != null && count > 0;
    }

    public void enrollStudent(Long studentId, Long sectionId) {
        jdbcTemplate.update(CALL_ENROLL_STUDENT, studentId, sectionId);
    }

    public boolean cancelAndRestoreSeat(Long enrollmentId) {
        List<Long> updatedSections = jdbcTemplate.query(
                CANCEL_AND_RESTORE_SEAT,
                (rs, rowNum) -> rs.getLong("id"),
                enrollmentId
        );
        return !updatedSections.isEmpty();
    }

    public List<NearbySectionResponse> findNearbySections(Long subjectId, Double lat, Double lng) {
        String sql = """
            SELECT s.id AS section_id,
                s.id AS section_code,
                r.id AS room_id,
                r.name AS room_name,
                b.name AS building_name,
                ST_Distance(r.geom::geography, ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography) AS distance_m
            FROM section s
            JOIN room r ON s.room_id = r.id
            JOIN building b ON r.building_id = b.id
            WHERE s.subject_id = ?
            AND s.available_seats > 0
            AND s.semester_id = (
                SELECT semester_id
                FROM section
                WHERE subject_id = ?
                    AND semester_id IN (SELECT id FROM semester WHERE status = 'IN_PROGRESS')
                ORDER BY semester_id DESC
                LIMIT 1
            )
            ORDER BY distance_m ASC
            """;

        return jdbcTemplate.query(sql,
            new Object[]{lng, lat, subjectId, subjectId},
            (rs, rowNum) -> new NearbySectionResponse(
                rs.getLong("section_id"),
                rs.getString("section_code"),
                rs.getLong("room_id"),
                rs.getString("room_name"),
                rs.getString("building_name"),
                rs.getDouble("distance_m")
        ));
    }
}
