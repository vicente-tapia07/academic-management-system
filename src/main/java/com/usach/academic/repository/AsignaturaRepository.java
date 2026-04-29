package com.usach.academic.repository;

import com.usach.academic.model.AsignaturaEntity;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Repositorio para acceder a la tabla asignatura usando JDBC directo
@Repository
public class AsignaturaRepository {

    private final DataSource dataSource;

    public AsignaturaRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // mapea una fila del ResultSet a un objeto AsignaturaEntity
    private AsignaturaEntity mapRow(ResultSet rs) throws SQLException {
        AsignaturaEntity a = new AsignaturaEntity();
        a.setId(rs.getLong("id"));
        a.setCodigo(rs.getString("codigo"));
        a.setNombre(rs.getString("nombre"));
        a.setCreditos(rs.getInt("creditos"));
        a.setCarreraId(rs.getLong("carrera_id"));
        return a;
    }

    // retorna todas las asignaturas
    public List<AsignaturaEntity> findAll() {
        String sql = "SELECT * FROM asignatura";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<AsignaturaEntity> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(mapRow(rs));
            }
            return lista;

        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener asignaturas", e);
        }
    }

    // busca una asignatura por su id
    public Optional<AsignaturaEntity> findById(Long id) {
        String sql = "SELECT * FROM asignatura WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar asignatura por id", e);
        }
    }

    // guarda una nueva asignatura y retorna el id generado
    public AsignaturaEntity save(AsignaturaEntity asignatura) {
        String sql = "INSERT INTO asignatura (codigo, nombre, creditos, carrera_id) VALUES (?, ?, ?, ?) RETURNING id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, asignatura.getCodigo());
            ps.setString(2, asignatura.getNombre());
            ps.setInt(3, asignatura.getCreditos());
            ps.setLong(4, asignatura.getCarreraId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                asignatura.setId(rs.getLong("id"));
            }
            return asignatura;

        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar asignatura", e);
        }
    }

    // actualiza los datos de una asignatura existente
    public void update(AsignaturaEntity asignatura) {
        String sql = "UPDATE asignatura SET codigo = ?, nombre = ?, creditos = ?, carrera_id = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, asignatura.getCodigo());
            ps.setString(2, asignatura.getNombre());
            ps.setInt(3, asignatura.getCreditos());
            ps.setLong(4, asignatura.getCarreraId());
            ps.setLong(5, asignatura.getId());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar asignatura", e);
        }
    }

    // elimina una asignatura por su id
    public void delete(Long id) {
        String sql = "DELETE FROM asignatura WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar asignatura", e);
        }
    }
}