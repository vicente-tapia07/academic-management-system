package usach.cl.demo.repository;

import usach.cl.demo.entity.Professor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ProfessorRepository {
    private final JdbcClient jdbcClient;

    public ProfessorRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Professor save(Professor professor) {
        jdbcClient.sql("INSERT INTO professor (user_id, department) VALUES (?, ?)")
                .params(professor.getId(), professor.getDepartment())
                .update();
        return professor;
    }

    public Professor findByUserId(int userId) {
        return jdbcClient.sql("""
                SELECT u.id, u.name, u.email, u.password, u.role,
                       p.department
                FROM usuario u
                INNER JOIN professor p ON u.id = p.user_id
                WHERE u.id = ?
                """)
                .params(userId)
                .query((rs, rowNum) -> new Professor(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getString("department")
                ))
                .single();
    }

    public List<Professor> findAll() {
        return jdbcClient.sql("""
                SELECT u.id, u.name, u.email, u.password, u.role,
                       p.department
                FROM usuario u
                INNER JOIN professor p ON u.id = p.user_id
                """)
                .query((rs, rowNum) -> new Professor(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getString("department")
                ))
                .list();
    }

    public void updateProfessor(int userId, String department) {
        jdbcClient.sql("UPDATE professor SET department = ? WHERE user_id = ?")
                .params(department, userId)
                .update();
    }

    public void deleteByUserId(int userId) {
        jdbcClient.sql("DELETE FROM professor WHERE user_id = ?").params(userId).update();
    }
}