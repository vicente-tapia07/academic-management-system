package usach.cl.demo.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import usach.cl.demo.model.Student;

import java.util.List;
import java.util.Optional;

@Repository
public class StudentRepository {

    private static final String FIND_ALL = "SELECT * FROM estudiante";

    private static final String FIND_BY_ID = "SELECT * FROM estudiante WHERE id = ?";

    private static final String INSERT = "INSERT INTO estudiante (usuario_id, matricula, nombre, apellido, estado_academico) " + "VALUES (?, ?, ?, ?, ?)";

    private static final String UPDATE = "UPDATE estudiante SET nombre = ?, apellido = ?, estado_academico = ? " + "WHERE id = ?";

    private static final String DELETE_BY_ID = "DELETE FROM estudiante WHERE id = ?";

    private final JdbcTemplate jdbcTemplate;

    public StudentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Student> estudianteMapper = (rs, rowNum) -> {
        Student e = new Student();
        e.setId(rs.getLong("id"));
        e.setUserId(rs.getLong("usuario_id"));
        e.setEnrollment(rs.getString("matricula"));
        e.setFirstName(rs.getString("nombre"));
        e.setLastName(rs.getString("apellido"));
        e.setAcademicStatus(rs.getString("estado_academico"));
        return e;
    };

    public List<Student> findAll() {
        return jdbcTemplate.query(FIND_ALL, estudianteMapper);
    }

    public Optional<Student> findById(Long id) {
        List<Student> result = jdbcTemplate.query(FIND_BY_ID, estudianteMapper, id);
        return result.stream().findFirst();
    }

    public int save(Student e) {
        return jdbcTemplate.update(INSERT,
                e.getUserId(),
                e.getEnrollment(),
                e.getFirstName(),
                e.getLastName(),
                e.getAcademicStatus());
    }

    public int update(Student e) {
        return jdbcTemplate.update(UPDATE,
                e.getFirstName(),
                e.getLastName(),
                e.getAcademicStatus(),
                e.getId());
    }

    public int deleteById(Long id) {
        return jdbcTemplate.update(DELETE_BY_ID, id);
    }
}