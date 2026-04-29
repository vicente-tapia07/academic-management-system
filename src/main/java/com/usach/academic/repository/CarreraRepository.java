package com.usach.academic.repository;

import com.usach.academic.model.CarreraEntity;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Repositorio para acceder a la tabla carrera usando JDBC directo
@Repository
public class CarreraRepository {

    private final DataSource dataSource;

    public CarreraRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // mapea una fila del ResultSet a un objeto CarreraEntity
    private CarreraEntity mapRow(ResultSet rs) throws SQLException {
        CarreraEntity c = new CarreraEntity();
        c.setId(rs.getLong("id"));
        c.setCodigo(rs.getString("codigo"));
        c.setNombre(rs.getString("nombre"));
        return c;
    }

    // retorna todas las carreras
    public List<CarreraEntity> findAll() {
        String sql = "SELECT * FROM carrera";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<CarreraEntity> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(mapRow(rs));
            }
            return lista;

        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener carreras", e);
        }
    }

    // busca una carrera por su id
    public Optional<CarreraEntity> findById(Long id) {
        String sql = "SELECT * FROM carrera WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar carrera por id", e);
        }
    }

    // guarda una nueva carrera y retorna el id generado
    public CarreraEntity save(CarreraEntity carrera) {
        String sql = "INSERT INTO carrera (codigo, nombre) VALUES (?, ?) RETURNING id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, carrera.getCodigo());
            ps.setString(2, carrera.getNombre());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                carrera.setId(rs.getLong("id"));
            }
            return carrera;

        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar carrera", e);
        }
    }

    // actualiza los datos de una carrera existente
    public void update(CarreraEntity carrera) {
        String sql = "UPDATE carrera SET codigo = ?, nombre = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, carrera.getCodigo());
            ps.setString(2, carrera.getNombre());
            ps.setLong(3, carrera.getId());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar carrera", e);
        }
    }

    // elimina una carrera por su id
    public void delete(Long id) {
        String sql = "DELETE FROM carrera WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar carrera", e);
        }
    }
}