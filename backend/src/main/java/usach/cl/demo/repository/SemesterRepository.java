package usach.cl.demo.repository;

import usach.cl.demo.model.SemesterEntity;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// repositorio para acceder a la tabla semester usando JDBC directo
@Repository
public class SemesterRepository {

    private final DataSource dataSource;

    public SemesterRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private SemesterEntity mapRow(ResultSet rs) throws SQLException {
        SemesterEntity semester = new SemesterEntity();
        semester.setId(rs.getLong("id"));
        semester.setYear(rs.getInt("year"));
        semester.setPeriod(rs.getString("period"));
        semester.setStartDate(rs.getDate("start_date").toLocalDate());
        semester.setEndDate(rs.getDate("end_date").toLocalDate());
        semester.setGradeStartDate(rs.getDate("grade_start_date").toLocalDate());
        semester.setGradeEndDate(rs.getDate("grade_end_date").toLocalDate());
        semester.setStatus(rs.getString("status"));
        return semester;
    }

    public List<SemesterEntity> findAll() {
        String sql = "SELECT * FROM semester";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<SemesterEntity> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;

        } catch (SQLException e) {
            throw new RuntimeException("Error fetching semesters", e);
        }
    }

    public Optional<SemesterEntity> findById(Long id) {
        String sql = "SELECT * FROM semester WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Error fetching semester by id", e);
        }
    }

    public SemesterEntity save(SemesterEntity semester) {
        String sql = "INSERT INTO semester (year, period, start_date, end_date, grade_start_date, grade_end_date, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, semester.getYear());
            ps.setString(2, semester.getPeriod());
            ps.setDate(3, Date.valueOf(semester.getStartDate()));
            ps.setDate(4, Date.valueOf(semester.getEndDate()));
            ps.setDate(5, Date.valueOf(semester.getGradeStartDate()));
            ps.setDate(6, Date.valueOf(semester.getGradeEndDate()));
            ps.setString(7, semester.getStatus());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                semester.setId(rs.getLong("id"));
            }
            return semester;

        } catch (SQLException e) {
            throw new RuntimeException("Error saving semester", e);
        }
    }

    public void update(SemesterEntity semester) {
        String sql = "UPDATE semester SET year = ?, period = ?, start_date = ?, end_date = ?, " +
                "grade_start_date = ?, grade_end_date = ?, status = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, semester.getYear());
            ps.setString(2, semester.getPeriod());
            ps.setDate(3, Date.valueOf(semester.getStartDate()));
            ps.setDate(4, Date.valueOf(semester.getEndDate()));
            ps.setDate(5, Date.valueOf(semester.getGradeStartDate()));
            ps.setDate(6, Date.valueOf(semester.getGradeEndDate()));
            ps.setString(7, semester.getStatus());
            ps.setLong(8, semester.getId());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error updating semester", e);
        }
    }

    public void closeSemester(Long semesterId) {
        String sql = "CALL sp_close_semester(?)";
        try (Connection conn = dataSource.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setLong(1, semesterId);
            cs.execute();

        } catch (SQLException e) {
            throw new RuntimeException("Error closing semester", e);
        }
    }
}