package usach.cl.demo.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import usach.cl.demo.dto.NearestRoomResponseDTO;

import java.util.List;
import java.util.Optional;

@Repository
public class LocationRepository {

    // No toca SectionEntity/SectionRepository (de otro dueño): consulta directo
    // las columnas físicas room_id/day_of_week/start_time/end_time que ya existen
    // en la tabla `section` aunque SectionEntity todavía no las exponga.
    //
    // day_of_week en la tabla usa 0=domingo..6=sábado, que coincide exactamente
    // con EXTRACT(DOW FROM ...) de Postgres, así que no hace falta traducir nada.
    //
    // El ORDER BY usa el operador <-> (KNN) sobre room.geom, que aprovecha el
    // índice GIST (idx_room_geom) en vez de calcular y ordenar todas las distancias.
    private static final String FIND_NEAREST_ACTIVE_ROOM =
            "SELECT r.id AS room_id, r.code AS room_code, r.name AS room_name, " +
            "       r.building_id AS building_id, sec.id AS section_id, " +
            "       sub.id AS subject_id, sub.name AS subject_name, " +
            "       ST_Distance(r.geom::geography, ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography) AS distance_m, " +
            "       ST_AsGeoJSON(r.geom) AS geom_json " +
            "FROM room r " +
            "JOIN section sec ON sec.room_id = r.id " +
            "JOIN enrollment e ON e.section_id = sec.id " +
            "JOIN subject sub ON sub.id = sec.subject_id " +
            "WHERE e.student_id = ? " +
            "  AND e.status = 'ACTIVE' " +
            "  AND sec.day_of_week = EXTRACT(DOW FROM CURRENT_TIMESTAMP) " +
            "  AND CURRENT_TIME BETWEEN sec.start_time AND sec.end_time " +
            "ORDER BY r.geom <-> ST_SetSRID(ST_MakePoint(?, ?), 4326) " +
            "LIMIT 1";

    private final JdbcTemplate jdbcTemplate;

    public LocationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<NearestRoomResponseDTO> nearestRoomMapper = (rs, rowNum) -> {
        NearestRoomResponseDTO dto = new NearestRoomResponseDTO();
        dto.setRoomId(rs.getLong("room_id"));
        dto.setRoomCode(rs.getString("room_code"));
        dto.setRoomName(rs.getString("room_name"));
        dto.setBuildingId(rs.getLong("building_id"));
        dto.setSectionId(rs.getLong("section_id"));
        dto.setSubjectId(rs.getLong("subject_id"));
        dto.setSubjectName(rs.getString("subject_name"));
        dto.setDistanceMeters(rs.getDouble("distance_m"));
        dto.setGeomGeoJson(rs.getString("geom_json"));
        return dto;
    };

    public Optional<NearestRoomResponseDTO> findNearestActiveRoom(Long studentId, Double lat, Double lng) {
        List<NearestRoomResponseDTO> result = jdbcTemplate.query(
                FIND_NEAREST_ACTIVE_ROOM,
                nearestRoomMapper,
                lng, lat,      // para ST_Distance (geography)
                studentId,
                lng, lat       // para el ORDER BY <-> (KNN con índice GIST)
        );
        return result.stream().findFirst();
    }
}
