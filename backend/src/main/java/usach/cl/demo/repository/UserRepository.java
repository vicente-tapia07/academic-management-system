package usach.cl.demo.repository;

import usach.cl.demo.model.Role;
import jakarta.annotation.Nonnull;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import usach.cl.demo.model.UserEntity;
import java.util.List;

@Repository
public class UserRepository {

    private final JdbcClient jdbcClient;

    public UserRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public UserEntity findByEmail(@Nonnull String email) {
        return jdbcClient.sql("SELECT * FROM usuario WHERE email = ?")
                .params(email)
                .query(this::mapRowToUser)
                .single();
    }

    public UserEntity findById(int id) {
        return jdbcClient.sql("SELECT * FROM usuario WHERE id = ?")
                .params(id)
                .query(this::mapRowToUser)
                .single();
    }

    public List<UserEntity> findAllByRole(Role role) {
        return jdbcClient.sql("SELECT * FROM usuario WHERE rol = ?")
                .params(role.name())
                .query(this::mapRowToUser)
                .list();
    }

    public UserEntity save(@Nonnull UserEntity user) {
        jdbcClient.sql(
                        "INSERT INTO usuario (rut, email, password_hash, rol) VALUES (?, ?, ?, ?)")
                .params(
                        user.getRut(),
                        user.getEmail(),
                        user.getPassword(),
                        user.getRole().name()
                )
                .update();
        return user;
    }

    public void updateUser(int id, String email, String rol) {
        jdbcClient.sql("UPDATE usuario SET email = ?, rol = ? WHERE id = ?")
                .params(email, rol, id)
                .update();
    }

    public void deleteById(int id) {
        jdbcClient.sql("DELETE FROM usuario WHERE id = ?")
                .params(id)
                .update();
    }

    private UserEntity mapRowToUser(java.sql.ResultSet rs, int rowNum)
            throws java.sql.SQLException {
        return new UserEntity(
                (int) rs.getLong("id"),
                rs.getString("rut"),
                rs.getString("email"),
                rs.getString("password_hash"),
                Role.valueOf(rs.getString("rol"))
        );
    }
}