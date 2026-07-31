package usach.cl.demo.repository;

import usach.cl.demo.model.SubjectEntity;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class SubjectRepository {

    private final DataSource dataSource;

    public SubjectRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private SubjectEntity mapRow(ResultSet rs) throws SQLException {
        SubjectEntity subject = new SubjectEntity();
        subject.setId(rs.getLong("id"));
        subject.setCode(rs.getString("code"));
        subject.setName(rs.getString("name"));
        subject.setCredits(rs.getInt("credits"));
        subject.setCareerId(rs.getLong("career_id"));
        subject.setActive(rs.getBoolean("active"));
        return subject;
    }

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

    public SubjectEntity save(SubjectEntity subject) {
        String sql = "INSERT INTO subject (code, name, credits, career_id, active) VALUES (?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, subject.getCode());
            ps.setString(2, subject.getName());
            ps.setInt(3, subject.getCredits());
            ps.setLong(4, subject.getCareerId());
            ps.setBoolean(5, subject.isActive());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) subject.setId(rs.getLong("id"));
            return subject;
        } catch (SQLException e) {
            throw new RuntimeException("Error saving subject", e);
        }
    }

    public void update(SubjectEntity subject) {
        String sql = "UPDATE subject SET code = ?, name = ?, credits = ?, career_id = ?, active = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, subject.getCode());
            ps.setString(2, subject.getName());
            ps.setInt(3, subject.getCredits());
            ps.setLong(4, subject.getCareerId());
            ps.setBoolean(5, subject.isActive());
            ps.setLong(6, subject.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating subject", e);
        }
    }

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
