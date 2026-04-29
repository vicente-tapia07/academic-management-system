package com.usach.academic.repository;

import com.usach.academic.model.PrerequistoEntity;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// Repositorio para acceder a la tabla prerequisito usando JDBC directo
@Repository
public class PrerequistoRepository {

    private final DataSource dataSource;

    public PrerequistoRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // retorna todos los prerequisitos de una asignatura
    public List<PrerequistoEntity> findByAsignaturaId(Long asignaturaId) {
        String sql = "SELECT * FROM prerequisito WHERE asignatura_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, asignaturaId);
            ResultSet rs = ps.executeQuery();
            List<PrerequistoEntity> lista = new ArrayList<>();
            while (rs.next()) {
                PrerequistoEntity p = new PrerequistoEntity();
                p.setAsignaturaId(rs.getLong("asignatura_id"));
                p.setAsignaturaPrerequistoId(rs.getLong("asignatura_prerequisito_id"));
                lista.add(p);
            }
            return lista;

        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener prerequisitos", e);
        }
    }

    // guarda un nuevo prerequisito
    public void save(PrerequistoEntity prerequisto) {
        String sql = "INSERT INTO prerequisito (asignatura_id, asignatura_prerequisito_id) VALUES (?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, prerequisto.getAsignaturaId());
            ps.setLong(2, prerequisto.getAsignaturaPrerequistoId());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar prerequisito", e);
        }
    }

    // elimina un prerequisito
    public void delete(Long asignaturaId, Long prerequisitoId) {
        String sql = "DELETE FROM prerequisito WHERE asignatura_id = ? AND asignatura_prerequisito_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, asignaturaId);
            ps.setLong(2, prerequisitoId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar prerequisito", e);
        }
    }
}