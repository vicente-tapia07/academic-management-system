package usach.cl.demo.repository;

import usach.cl.demo.model.ProfessorEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ProfessorRepository {

    private final JdbcClient jdbcClient;

    public ProfessorRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public ProfessorEntity save(ProfessorEntity professor) {
        jdbcClient.sql(
            "INSERT INTO professor (usuario_id, department, first_name, last_name) " +
            "VALUES (?, ?, ?, ?)")
            .params(professor.getUsuarioId(), professor.getDepartment(),
                    professor.getFirstName(), professor.getLastName())
            .update();
        return professor;
    }

    public ProfessorEntity findById(Long id) {
        return jdbcClient.sql("""
            SELECT id, usuario_id, first_name, last_name, department
            FROM professor WHERE id = ?
            """)
            .params(id)
            .query((rs, rowNum) -> new ProfessorEntity(
                rs.getLong("id"),
                rs.getLong("usuario_id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("department")
            ))
            .single();
    }

    public ProfessorEntity findByUserId(Long usuarioId) {
        return jdbcClient.sql("""
            SELECT id, usuario_id, first_name, last_name, department
            FROM professor WHERE usuario_id = ?
            """)
            .params(usuarioId)
            .query((rs, rowNum) -> new ProfessorEntity(
                rs.getLong("id"),
                rs.getLong("usuario_id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("department")
            ))
            .single();
    }

    public List<ProfessorEntity> findAll() {
        return jdbcClient.sql("""
            SELECT id, usuario_id, first_name, last_name, department
            FROM professor ORDER BY last_name, first_name
            """)
            .query((rs, rowNum) -> new ProfessorEntity(
                rs.getLong("id"),
                rs.getLong("usuario_id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("department")
            ))
            .list();
    }

    // Corregido: usa WHERE id = ? (id del profesor, no usuario_id)
    public void updateProfessor(Long professorId, String department,
                                String firstName, String lastName) {
        jdbcClient.sql(
            "UPDATE professor SET department = ?, first_name = ?, last_name = ? WHERE id = ?")
            .params(department, firstName, lastName, professorId)
            .update();
    }

    public void deleteByUserId(Long usuarioId) {
        jdbcClient.sql("DELETE FROM professor WHERE usuario_id = ?")
            .params(usuarioId).update();
    }
}
