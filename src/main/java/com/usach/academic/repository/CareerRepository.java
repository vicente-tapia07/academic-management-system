package com.usach.academic.repository;

import com.usach.academic.model.CareerEntity;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// repositorio para acceder a la tabla career usando JDBC directo
@Repository
public class CareerRepository {

    private final DataSource dataSource;

    public CareerRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // mapea una fila del ResultSet a un objeto CareerEntity
    private CareerEntity mapRow(ResultSet rs) throws SQLException {
        CareerEntity career = new CareerEntity();
        career.setId(rs.getLong("id"));
        career.setCode(rs.getString("code"));
        career.setName(rs.getString("name"));
        return career;
    }

    // retorna todas las carreras
    public List<CareerEntity> findAll() {
        String sql = "SELECT * FROM career";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<CareerEntity> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;

        } catch (SQLException e) {
            throw new RuntimeException("Error fetching careers", e);
        }
    }

    // busca una carrera por su id
    public Optional<CareerEntity> findById(Long id) {
        String sql = "SELECT * FROM career WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Error fetching career by id", e);
        }
    }

    // retorna todas las asignaturas de una carrera especifica
    public List<CareerEntity> findByCode(String code) {
        String sql = "SELECT * FROM career WHERE code = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, code);
            ResultSet rs = ps.executeQuery();
            List<CareerEntity> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;

        } catch (SQLException e) {
            throw new RuntimeException("Error fetching career by code", e);
        }
    }

    // guarda una nueva carrera y retorna el id generado
    public CareerEntity save(CareerEntity career) {
        String sql = "INSERT INTO career (code, name) VALUES (?, ?) RETURNING id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, career.getCode());
            ps.setString(2, career.getName());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                career.setId(rs.getLong("id"));
            }
            return career;

        } catch (SQLException e) {
            throw new RuntimeException("Error saving career", e);
        }
    }

    // actualiza los datos de una carrera existente
    public void update(CareerEntity career) {
        String sql = "UPDATE career SET code = ?, name = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, career.getCode());
            ps.setString(2, career.getName());
            ps.setLong(3, career.getId());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error updating career", e);
        }
    }

    // elimina una carrera por su id
    public void delete(Long id) {
        String sql = "DELETE FROM career WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error deleting career", e);
        }
    }
}