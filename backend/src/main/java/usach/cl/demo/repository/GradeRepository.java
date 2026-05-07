package usach.cl.demo.repository;


import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import usach.cl.demo.dto.GradeDTO;
import usach.cl.demo.model.GradeEntity;
import java.util.List;

@Repository
public class GradeRepository {

    private final JdbcTemplate jdbcTemplate;

    public GradeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<GradeEntity> gradeEntityMapper = (rs, rowNum) -> {
        GradeEntity g = new GradeEntity();
        g.setId(rs.getLong("id"));
        g.setEnrollmentId(rs.getLong("enrollment_id"));
        g.setValue(rs.getDouble("value"));
        g.setEntryDate(rs.getDate("entry_date").toLocalDate());
        return g;
    };

    private final RowMapper<GradeDTO> gradeDTOMapper = (rs, rowNum) -> {
        GradeDTO dto = new GradeDTO();
        dto.setGradeId(rs.getLong("grade_id"));
        dto.setValue(rs.getDouble("value"));
        dto.setEntryDate(rs.getDate("entry_date").toLocalDate());
        dto.setSubjectId(rs.getLong("subject_id"));
        dto.setSubjectCode(rs.getString("subject_code"));
        dto.setSubjectName(rs.getString("subject_name"));
        return dto;
    };

    public List<GradeEntity> findAll() {
        return jdbcTemplate.query("SELECT * FROM grade", gradeEntityMapper);
    }

    public GradeEntity save(GradeEntity g) {
        if (g.getId() == null) {
            jdbcTemplate.update(
                    "INSERT INTO grade (enrollment_id, value, entry_date) VALUES (?, ?, ?)",
                    g.getEnrollmentId(), g.getValue(), g.getEntryDate()
            );
        } else {
            jdbcTemplate.update(
                    "UPDATE grade SET enrollment_id = ?, value = ?, entry_date = ? WHERE id = ?",
                    g.getEnrollmentId(), g.getValue(), g.getEntryDate(), g.getId()
            );
        }
        return g;
    }

    public List<GradeDTO> findByStudentId(Long studentId) {
        String sql =
                "SELECT g.id AS grade_id, g.value, g.entry_date, " +
                        "sub.id AS subject_id, sub.code AS subject_code, sub.name AS subject_name " +
                        "FROM grade g " +
                        "JOIN enrollment e ON e.id = g.enrollment_id " +
                        "JOIN section sec  ON sec.id = e.section_id " +
                        "JOIN subject sub  ON sub.id = sec.subject_id " +
                        "WHERE e.student_id = ?";
        return jdbcTemplate.query(sql, gradeDTOMapper, studentId);
    }
}