package com.usach.academic.repository;

import com.usach.academic.model.SeccionEntity;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Repositorio para acceder a la tabla seccion usando JDBC directo
@Repository
public class SeccionRepository {

    private final DataSource dataSource;

    public SeccionRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // mapea una fila del ResultSet a un objeto SeccionEntity
    private SeccionEntity mapRow(ResultSet rs) throws SQLException {
        SeccionEntity s = new SeccionEntity();
        s.setId(rs.getLong("id"));
        s.setAsignaturaId(rs.getLong("asignatura_id"));
        s.setProfesorId(rs.getLong("profesor_id"));
        s.setSemestreId(rs.getLong("semestre_id"));
        s.setCuposTotal(rs.getInt("cupos_total"));
        s.setCuposDisponibles(rs.getInt("cupos_disponibles"));
        return s;
    }

    // retorna todas las secciones
    public List<SeccionEntity> findAll() {
        String sql = "SELECT * FROM seccion";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<SeccionEntity> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(mapRow(rs));
            }
            return lista;

        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener secciones", e);
        }
    }

    // busca una seccion por su id
    public Optional<SeccionEntity> findById(Long id) {
        String sql = "SELECT * FROM seccion WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar seccion por id", e);
        }
    }

    // guarda una nueva seccion y retorna el id generado
    public SeccionEntity save(SeccionEntity seccion) {
        String sql = "INSERT INTO seccion (asignatura_id, profesor_id, semestre_id, cupos_total, cupos_disponibles) " +
                "VALUES (?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, seccion.getAsignaturaId());
            ps.setLong(2, seccion.getProfesorId());
            ps.setLong(3, seccion.getSemestreId());
            ps.setInt(4, seccion.getCuposTotal());
            ps.setInt(5, seccion.getCuposDisponibles());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                seccion.setId(rs.getLong("id"));
            }
            return seccion;

        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar seccion", e);
        }
    }
}