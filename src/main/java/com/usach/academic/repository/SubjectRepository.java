package com.usach.academic.repository;

import com.usach.academic.model.SubjectEntity;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// repositorio para acceder a la tabla subject usando JDBC directo
@Repository
public class SubjectRepository {

    private final DataSource dataSource;

    public SubjectRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // mapea una fila del ResultSet a un objeto SubjectEntity
    private SubjectEntity mapRow(ResultSet rs) throws SQLException {
        SubjectEntity subject = new SubjectEntity();
        subject.setId(rs.getLong("id"));
        subject.setCode(rs.getString("code"));
        subject.setName(rs.getString("name"));
        subject.setCredits(rs.getInt("credits"));
        subject.setCareerId(rs.getLong("career_id"));
        return subject;
    }

    // retorna todas las asignaturas
    public List<SubjectEntity> findAll() {
        String sql = "SELECT * FROM subject";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<SubjectEntity> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;

        } catch (SQLException e) {
            throw new RuntimeException("Error fetching subjects", e);
        }
    }

    // busca una asignatura por su id
    public Optional<SubjectEntity> findById(Long id) {
        String sql = "SELECT * FROM subject WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Error fetching subject by id", e);
        }
    }

    // retorna todas las asignaturas de una carrera especifica
    public List<SubjectEntity> findByCareerId(Long careerId) {
        String sql = "SELECT * FROM subject WHERE career_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, careerId);
            ResultSet rs = ps.executeQuery();
            List<SubjectEntity> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;

        } catch (SQLException e) {
            throw new RuntimeException("Error fetching subjects by career", e);
        }
    }

    // guarda una nueva asignatura y retorna el id generado
    public SubjectEntity save(SubjectEntity subject) {
        String sql = "INSERT INTO subject (code, name, credits, career_id) VALUES (?, ?, ?, ?) RETURNING id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, subject.getCode());
            ps.setString(2, subject.getName());
            ps.setInt(3, subject.getCredits());
            ps.setLong(4, subject.getCareerId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                subject.setId(rs.getLong("id"));
            }
            return subject;

        } catch (SQLException e) {
            throw new RuntimeException("Error saving subject", e);
        }
    }

    // actualiza los datos de una asignatura existente
    public void update(SubjectEntity subject) {
        String sql = "UPDATE subject SET code = ?, name = ?, credits = ?, career_id = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, subject.getCode());
            ps.setString(2, subject.getName());
            ps.setInt(3, subject.getCredits());
            ps.setLong(4, subject.getCareerId());
            ps.setLong(5, subject.getId());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error updating subject", e);
        }
    }

    // elimina una asignatura por su id
    public void delete(Long id) {
        String sql = "DELETE FROM subject WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error deleting subject", e);
        }
    }
}