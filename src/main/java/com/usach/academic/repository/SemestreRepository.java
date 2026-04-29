package com.usach.academic.repository;

import com.usach.academic.model.SemestreEntity;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Repositorio para acceder a la tabla semestre usando JDBC directo
@Repository
public class SemestreRepository {

    private final DataSource dataSource;

    public SemestreRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // mapea una fila del ResultSet a un objeto SemestreEntity
    private SemestreEntity mapRow(ResultSet rs) throws SQLException {
        SemestreEntity s = new SemestreEntity();
        s.setId(rs.getLong("id"));
        s.setAnio(rs.getInt("anio"));
        s.setPeriodo(rs.getString("periodo"));
        s.setFechaInicio(rs.getDate("fecha_inicio").toLocalDate());
        s.setFechaFin(rs.getDate("fecha_fin").toLocalDate());
        s.setFechaInicioNotas(rs.getDate("fecha_inicio_notas").toLocalDate());
        s.setFechaFinNotas(rs.getDate("fecha_fin_notas").toLocalDate());
        s.setEstado(rs.getString("estado"));
        return s;
    }

    // retorna todos los semestres
    public List<SemestreEntity> findAll() {
        String sql = "SELECT * FROM semestre";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<SemestreEntity> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(mapRow(rs));
            }
            return lista;

        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener semestres", e);
        }
    }

    // busca un semestre por su id
    public Optional<SemestreEntity> findById(Long id) {
        String sql = "SELECT * FROM semestre WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar semestre por id", e);
        }
    }

    // guarda un nuevo semestre y retorna el id generado
    public SemestreEntity save(SemestreEntity semestre) {
        String sql = "INSERT INTO semestre (anio, periodo, fecha_inicio, fecha_fin, fecha_inicio_notas, fecha_fin_notas, estado) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, semestre.getAnio());
            ps.setString(2, semestre.getPeriodo());
            ps.setDate(3, Date.valueOf(semestre.getFechaInicio()));
            ps.setDate(4, Date.valueOf(semestre.getFechaFin()));
            ps.setDate(5, Date.valueOf(semestre.getFechaInicioNotas()));
            ps.setDate(6, Date.valueOf(semestre.getFechaFinNotas()));
            ps.setString(7, semestre.getEstado());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                semestre.setId(rs.getLong("id"));
            }
            return semestre;

        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar semestre", e);
        }
    }

    // actualiza los datos de un semestre existente
    public void update(SemestreEntity semestre) {
        String sql = "UPDATE semestre SET anio = ?, periodo = ?, fecha_inicio = ?, fecha_fin = ?, " +
                "fecha_inicio_notas = ?, fecha_fin_notas = ?, estado = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, semestre.getAnio());
            ps.setString(2, semestre.getPeriodo());
            ps.setDate(3, Date.valueOf(semestre.getFechaInicio()));
            ps.setDate(4, Date.valueOf(semestre.getFechaFin()));
            ps.setDate(5, Date.valueOf(semestre.getFechaInicioNotas()));
            ps.setDate(6, Date.valueOf(semestre.getFechaFinNotas()));
            ps.setString(7, semestre.getEstado());
            ps.setLong(8, semestre.getId());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar semestre", e);
        }
    }

    // llama al stored procedure de cierre de semestre
    public void cerrarSemestre(Long idSemestre) {
        String sql = "CALL sp_cierre_semestre(?)";
        try (Connection conn = dataSource.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setLong(1, idSemestre);
            cs.execute();

        } catch (SQLException e) {
            throw new RuntimeException("Error al cerrar semestre", e);
        }
    }
}