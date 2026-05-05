package usach.cl.demo.repository;

import usach.cl.demo.model.UserEntity;
import usach.cl.demo.model.Role;
import jakarta.annotation.Nonnull;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class UserRepository {

    private final JdbcClient jdbcClient;

    public UserRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    // Busca usuario por email para el login
    public UserEntity findByEmail(@Nonnull String email) {
        return jdbcClient.sql("SELECT * FROM usuario WHERE email = ?")
                .params(email)
                .query(this::mapRowToUser)
                .single();
    }

    // Busca usuario por id
    public UserEntity findById(int id) {
        return jdbcClient.sql("SELECT * FROM usuario WHERE id = ?")
                .params(id)
                .query(this::mapRowToUser)
                .single();
    }

    // Retorna todos los usuarios por rol
    public List<UserEntity> findAllByRole(Role role) {
        return jdbcClient.sql("SELECT * FROM usuario WHERE rol = ?")
                .params(role.name())
                .query(this::mapRowToUser)
                .list();
    }

    // Convierte una fila SQL en UserEntity
    private UserEntity mapRowToUser(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new UserEntity(
                (int) rs.getLong("id"),
                rs.getString("rut"),
                rs.getString("email"),
                rs.getString("password_hash"),
                Role.valueOf(rs.getString("rol"))
        );
    }
}