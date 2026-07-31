package usach.cl.demo.repository;

import usach.cl.demo.model.SectionEntity;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class SectionRepository {

    private final DataSource dataSource;

    public SectionRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private SectionEntity mapRow(ResultSet rs) throws SQLException {
        SectionEntity section = new SectionEntity();
        section.setId(rs.getLong("id"));
        section.setSubjectId(rs.getLong("subject_id"));
        section.setProfessorId(rs.getLong("professor_id"));
        section.setSemesterId(rs.getLong("semester_id"));
        section.setTotalSeats(rs.getInt("total_seats"));
        section.setAvailableSeats(rs.getInt("available_seats"));

        // Campos del Lab 2
        long roomId = rs.getLong("room_id");
        if (!rs.wasNull()) section.setRoomId(roomId);

        int dow = rs.getInt("day_of_week");
        if (!rs.wasNull()) section.setDayOfWeek(dow);

        Time startTime = rs.getTime("start_time");
        if (startTime != null) section.setStartTime(startTime.toLocalTime());

        Time endTime = rs.getTime("end_time");
        if (endTime != null) section.setEndTime(endTime.toLocalTime());

        return section;
    }

    public List<SectionEntity> findAll() {
        String sql = "SELECT * FROM section";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<SectionEntity> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;

        } catch (SQLException e) {
            throw new RuntimeException("Error fetching sections", e);
        }
    }

    public Optional<SectionEntity> findById(Long id) {
        String sql = "SELECT * FROM section WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Error fetching section by id", e);
        }
    }

    public SectionEntity save(SectionEntity section) {
        String checkSql = """
        SELECT COUNT(*) FROM section
        WHERE semester_id = ?
        AND day_of_week = ?
        AND NOT (end_time <= ? OR start_time >= ?)
        AND (room_id = ? OR professor_id = ?)
        """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement check = conn.prepareStatement(checkSql)) {

            check.setLong(1, section.getSemesterId());
            check.setInt(2, section.getDayOfWeek());
            check.setTime(3, Time.valueOf(section.getStartTime()));
            check.setTime(4, Time.valueOf(section.getEndTime()));
            check.setLong(5, section.getRoomId());
            check.setLong(6, section.getProfessorId());
            ResultSet cr = check.executeQuery();
            if (cr.next() && cr.getInt(1) > 0) {
                throw new RuntimeException(
                        "Conflicto de horario: la sala o el profesor ya están ocupados en ese bloque."
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error verificando conflicto de sala", e);
        }

        String sql = "INSERT INTO section (subject_id, professor_id, semester_id, " +
                "total_seats, available_seats, room_id, day_of_week, start_time, end_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, section.getSubjectId());
            ps.setLong(2, section.getProfessorId());
            ps.setLong(3, section.getSemesterId());
            ps.setInt(4, section.getTotalSeats());
            ps.setInt(5, section.getAvailableSeats());
            ps.setLong(6, section.getRoomId());
            ps.setInt(7, section.getDayOfWeek());
            ps.setTime(8, section.getStartTime() != null
                    ? Time.valueOf(section.getStartTime()) : null);
            ps.setTime(9, section.getEndTime() != null
                    ? Time.valueOf(section.getEndTime()) : null);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) section.setId(rs.getLong("id"));
            return section;

        } catch (SQLException e) {
            throw new RuntimeException("Error saving section", e);
        }
    }

    public List<SectionEntity> findByProfessorId(Long professorId) {
        String sql = "SELECT * FROM section WHERE professor_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, professorId);
            ResultSet rs = ps.executeQuery();
            List<SectionEntity> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;

        } catch (SQLException e) {
            throw new RuntimeException("Error fetching sections by professor", e);
        }
    }

    public SectionEntity update(SectionEntity section) {
        String checkSql = """
        SELECT COUNT(*) FROM section
        WHERE semester_id = ?
        AND day_of_week = ?
        AND id != ?
        AND NOT (end_time <= ? OR start_time >= ?)
        AND (room_id = ? OR professor_id = ?)
        """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement check = conn.prepareStatement(checkSql)) {

            check.setLong(1, section.getSemesterId());
            check.setInt(2, section.getDayOfWeek());
            check.setLong(3, section.getId());
            check.setTime(4, Time.valueOf(section.getStartTime()));
            check.setTime(5, Time.valueOf(section.getEndTime()));
            check.setLong(6, section.getRoomId());
            check.setLong(7, section.getProfessorId());
            ResultSet cr = check.executeQuery();
            if (cr.next() && cr.getInt(1) > 0) {
                throw new RuntimeException(
                        "Conflicto de horario: la sala o el profesor ya están ocupados en ese bloque."
                );
            }
        } catch (SQLException e) {
            if (e.getMessage() == null || !e.getMessage().contains("Conflicto")) {
                throw new RuntimeException("Error verificando conflicto de sala", e);
            }
            throw new RuntimeException(e.getMessage());
        }

        String sql = """
        UPDATE section SET
            subject_id = ?, professor_id = ?, semester_id = ?,
            total_seats = ?, available_seats = ?,
            room_id = ?, day_of_week = ?, start_time = ?, end_time = ?
        WHERE id = ?
        """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, section.getSubjectId());
            ps.setLong(2, section.getProfessorId());
            ps.setLong(3, section.getSemesterId());
            ps.setInt(4, section.getTotalSeats());
            ps.setInt(5, section.getAvailableSeats());
            ps.setLong(6, section.getRoomId());
            ps.setInt(7, section.getDayOfWeek());
            ps.setTime(8, Time.valueOf(section.getStartTime()));
            ps.setTime(9, Time.valueOf(section.getEndTime()));
            ps.setLong(10, section.getId());
            ps.executeUpdate();
            return section;

        } catch (SQLException e) {
            throw new RuntimeException("Error updating section", e);
        }
    }

    public int deleteById(Long id) {
        String sql = "DELETE FROM section WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error deleting section", e);
        }
    }

    // Secciones de un estudiante (via enrollment) con semestre activo
    public List<SectionEntity> findByStudentId(Long studentId) {
        String sql = """
            SELECT s.*
            FROM section s
            JOIN enrollment e ON e.section_id = s.id
            JOIN semester sem ON sem.id = s.semester_id
            WHERE e.student_id = ?
            AND e.status = 'ACTIVE'
            AND sem.status = 'IN_PROGRESS'
            ORDER BY s.day_of_week, s.start_time
            """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, studentId);
            ResultSet rs = ps.executeQuery();
            List<SectionEntity> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;

        } catch (SQLException e) {
            throw new RuntimeException("Error fetching sections by student", e);
        }
    }

    // Secciones del profesor filtrando por semestre activo (IN_PROGRESS)
    public List<SectionEntity> findByProfessorIdAndActiveSemester(Long professorId) {
        String sql = """
            SELECT s.*
            FROM section s
            JOIN semester sem ON sem.id = s.semester_id
            WHERE s.professor_id = ?
            AND sem.status = 'IN_PROGRESS'
            ORDER BY s.day_of_week, s.start_time
            """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, professorId);
            ResultSet rs = ps.executeQuery();
            List<SectionEntity> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;

        } catch (SQLException e) {
            throw new RuntimeException("Error fetching active sections by professor", e);
        }
    }
}
