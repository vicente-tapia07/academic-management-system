package usach.cl.demo.repository;

import usach.cl.demo.model.PrerequisiteEntity;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class PrerequisiteRepository {

    private final DataSource dataSource;

    public PrerequisiteRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<PrerequisiteEntity> findBySubjectId(Long subjectId) {
        String sql = "SELECT * FROM prerequisite WHERE subject_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, subjectId);
            ResultSet rs = ps.executeQuery();
            List<PrerequisiteEntity> list = new ArrayList<>();
            while (rs.next()) {
                PrerequisiteEntity p = new PrerequisiteEntity();
                p.setSubjectId(rs.getLong("subject_id"));
                p.setPrerequisiteSubjectId(rs.getLong("prerequisite_subject_id"));
                list.add(p);
            }
            return list;

        } catch (SQLException e) {
            throw new RuntimeException("Error fetching prerequisites", e);
        }
    }

    public void save(PrerequisiteEntity prerequisite) {
        String sql = "INSERT INTO prerequisite (subject_id, prerequisite_subject_id) VALUES (?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, prerequisite.getSubjectId());
            ps.setLong(2, prerequisite.getPrerequisiteSubjectId());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error saving prerequisite", e);
        }
    }

    public void delete(Long subjectId, Long prerequisiteId) {
        String sql = "DELETE FROM prerequisite WHERE subject_id = ? AND prerequisite_subject_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, subjectId);
            ps.setLong(2, prerequisiteId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error deleting prerequisite", e);
        }
    }
}