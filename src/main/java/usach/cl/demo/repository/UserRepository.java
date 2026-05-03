package usach.cl.demo.repository;

import usach.cl.demo.entity.User;
import usach.cl.demo.model.Role;
import jakarta.annotation.Nonnull;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class UserRepository {
    private final JdbcClient jdbcClient;

    public UserRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public User save(@Nonnull User user) {
        var keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql("INSERT INTO users (name, email, password, role) VALUES (?, ?, ?, ?)")
                .params(user.getName(), user.getEmail(), user.getPassword(), user.getRole().name())
                .update(keyHolder);
        var map = keyHolder.getKeys();
        return userFromMap(map);
    }

    public List<User> findAllByRole(Role role) {
        return jdbcClient.sql("SELECT * FROM users WHERE role = ?")
                .params(role.name())
                .query(this::mapRowToUser)
                .list();
    }

    public User findById(int id) {
        return jdbcClient.sql("SELECT * FROM users WHERE id = ?")
                .params(id)
                .query(this::mapRowToUser)
                .single();
    }

    public User findByEmail(@Nonnull String email) {
        return jdbcClient.sql("SELECT * FROM users WHERE email = ?")
                .params(email)
                .query(this::mapRowToUser)
                .single();
    }

    public void updateUser(int id, String name, String email) {
        jdbcClient.sql("UPDATE users SET name = ?, email = ? WHERE id = ?")
                .params(name, email, id)
                .update();
    }

    public void deleteById(int id) {
        jdbcClient.sql("DELETE FROM users WHERE id = ?").params(id).update();
    }

    private User mapRowToUser(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new User(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("password"),
                Role.valueOf(rs.getString("role"))
        );
    }

    private User userFromMap(@Nonnull Map<String, Object> map) {
        return new User(
                (Integer) map.get("id"),
                (String) map.get("name"),
                (String) map.get("email"),
                (String) map.get("password"),
                Role.valueOf((String) map.get("role"))
        );
    }
}