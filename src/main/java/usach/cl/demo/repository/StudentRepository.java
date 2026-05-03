package usach.cl.demo.repository;

import usach.cl.demo.entity.Student;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class StudentRepository {
    private final JdbcClient jdbcClient;

    public StudentRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Student save(Student student) {
        jdbcClient.sql("INSERT INTO students (user_id, student_id, program) VALUES (?, ?, ?)")
                .params(student.getId(), student.getStudentId(), student.getProgram())
                .update();
        return student;
    }

    public Student findByUserId(int userId) {
        return jdbcClient.sql("""
                SELECT u.id, u.name, u.email, u.password, u.role,
                       s.student_id, s.program
                FROM users u
                INNER JOIN students s ON u.id = s.user_id
                WHERE u.id = ?
                """)
                .params(userId)
                .query((rs, rowNum) -> new Student(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getString("student_id"),
                        rs.getString("program")
                ))
                .single();
    }

    public List<Student> findAll() {
        return jdbcClient.sql("""
                SELECT u.id, u.name, u.email, u.password, u.role,
                       s.student_id, s.program
                FROM users u
                INNER JOIN students s ON u.id = s.user_id
                """)
                .query((rs, rowNum) -> new Student(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getString("student_id"),
                        rs.getString("program")
                ))
                .list();
    }

    public void updateStudent(int userId, String studentId, String program) {
        jdbcClient.sql("UPDATE students SET student_id = ?, program = ? WHERE user_id = ?")
                .params(studentId, program, userId)
                .update();
    }

    public void deleteByUserId(int userId) {
        jdbcClient.sql("DELETE FROM students WHERE user_id = ?").params(userId).update();
    }
}