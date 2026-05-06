package usach.cl.demo.repository;

import usach.cl.demo.model.SectionEntity;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// repositorio para acceder a la tabla section usando JDBC directo
@Repository
public class SectionRepository {

    private final DataSource dataSource;

    public SectionRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // mapea una fila del ResultSet a un objeto SectionEntity
    private SectionEntity mapRow(ResultSet rs) throws SQLException {
        SectionEntity section = new SectionEntity();
        section.setId(rs.getLong("id"));
        section.setSubjectId(rs.getLong("subject_id"));
        section.setProfessorId(rs.getLong("professor_id"));
        section.setSemesterId(rs.getLong("semester_id"));
        section.setTotalSeats(rs.getInt("total_seats"));
        section.setAvailableSeats(rs.getInt("available_seats"));
        return section;
    }

    // retorna todas las secciones
    public List<SectionEntity> findAll() {
        String sql = "SELECT * FROM section";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<SectionEntity> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;

        } catch (SQLException e) {
            throw new RuntimeException("Error fetching sections", e);
        }
    }

    // busca una seccion por su id
    public Optional<SectionEntity> findById(Long id) {
        String sql = "SELECT * FROM section WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Error fetching section by id", e);
        }
    }

    // guarda una nueva seccion y retorna el id generado
    public SectionEntity save(SectionEntity section) {
        String sql = "INSERT INTO section (subject_id, professor_id, semester_id, total_seats, available_seats) " +
                "VALUES (?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, section.getSubjectId());
            ps.setLong(2, section.getProfessorId());
            ps.setLong(3, section.getSemesterId());
            ps.setInt(4, section.getTotalSeats());
            ps.setInt(5, section.getAvailableSeats());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                section.setId(rs.getLong("id"));
            }
            return section;

        } catch (SQLException e) {
            throw new RuntimeException("Error saving section", e);
        }
    }

    // retorna todas las secciones de un profesor específico
    public List<SectionEntity> findByProfessorId(Long professorId) {
        String sql = "SELECT * FROM section WHERE professor_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, professorId);
            ResultSet rs = ps.executeQuery();
            List<SectionEntity> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;

        } catch (SQLException e) {
            throw new RuntimeException("Error fetching sections by professor", e);
        }
    }
}