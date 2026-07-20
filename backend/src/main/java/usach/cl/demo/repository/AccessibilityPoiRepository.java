package usach.cl.demo.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import usach.cl.demo.dto.AccessibleRoomDTO;
import usach.cl.demo.model.AccessibilityPoiEntity;

import java.util.List;
import java.util.Optional;

@Repository
public class AccessibilityPoiRepository {

    // Endpoint 2 (Accesibilidad): para cada room, busca la rampa (accessibility_poi)
    // más cercana usando el operador KNN <-> (aprovecha idx_ramp_geom / idx_room_geom),
    // y luego calcula la distancia real en metros con ST_Distance sobre geography
    // solo para esa rampa ganadora (LATERAL evita calcular todas las distancias).
    // accessible = true si esa distancia es <= 50 metros (regla del enunciado).
    // Si buildingId es null, retorna las salas de todos los edificios.
    private static final String FIND_ACCESSIBLE_ROOMS =
            "SELECT r.id AS room_id, r.code AS room_code, r.name AS room_name, " +
            "       r.building_id AS building_id, nearest.dist_m AS nearest_ramp_m " +
            "FROM room r " +
            "LEFT JOIN LATERAL ( " +
            "    SELECT ST_Distance(r.geom::geography, p.geom::geography) AS dist_m " +
            "    FROM accessibility_poi p " +
            "    ORDER BY r.geom <-> p.geom " +
            "    LIMIT 1 " +
            ") nearest ON true " +
            "WHERE (CAST(? AS BIGINT) IS NULL OR r.building_id = CAST(? AS BIGINT)) " +
            "ORDER BY r.code";

    private static final double ACCESSIBLE_RADIUS_METERS = 50.0;

    private static final String FIND_ALL =
            "SELECT id, name, building_id, ST_AsGeoJSON(geom) AS geom_json FROM accessibility_poi";

    private static final String FIND_BY_ID =
            "SELECT id, name, building_id, ST_AsGeoJSON(geom) AS geom_json FROM accessibility_poi WHERE id = ?";

    private static final String FIND_BY_BUILDING_ID =
            "SELECT id, name, building_id, ST_AsGeoJSON(geom) AS geom_json FROM accessibility_poi WHERE building_id = ?";

    private static final String INSERT =
            "INSERT INTO accessibility_poi (name, building_id, geom) " +
                    "VALUES (?, ?, ST_GeomFromGeoJSON(?))";

    private static final String UPDATE =
            "UPDATE accessibility_poi SET name = ?, building_id = ?, geom = ST_GeomFromGeoJSON(?) WHERE id = ?";

    private static final String DELETE_BY_ID =
            "DELETE FROM accessibility_poi WHERE id = ?";

    private final JdbcTemplate jdbcTemplate;

    public AccessibilityPoiRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<AccessibilityPoiEntity> poiMapper = (rs, rowNum) -> {
        AccessibilityPoiEntity p = new AccessibilityPoiEntity();
        p.setId(rs.getLong("id"));
        p.setName(rs.getString("name"));
        // building_id es nullable -> usar getObject para no convertir null en 0
        p.setBuildingId(rs.getObject("building_id", Long.class));
        p.setGeomGeoJson(rs.getString("geom_json"));
        return p;
    };

    public List<AccessibilityPoiEntity> findAll() {
        return jdbcTemplate.query(FIND_ALL, poiMapper);
    }

    public Optional<AccessibilityPoiEntity> findById(Long id) {
        List<AccessibilityPoiEntity> result = jdbcTemplate.query(FIND_BY_ID, poiMapper, id);
        return result.stream().findFirst();
    }

    public List<AccessibilityPoiEntity> findByBuildingId(Long buildingId) {
        return jdbcTemplate.query(FIND_BY_BUILDING_ID, poiMapper, buildingId);
    }

    public int save(AccessibilityPoiEntity poi) {
        return jdbcTemplate.update(INSERT,
                poi.getName(),
                poi.getBuildingId(),
                poi.getGeomGeoJson()
        );
    }

    public int update(AccessibilityPoiEntity poi) {
        return jdbcTemplate.update(UPDATE,
                poi.getName(),
                poi.getBuildingId(),
                poi.getGeomGeoJson(),
                poi.getId()
        );
    }

    public int deleteById(Long id) {
        return jdbcTemplate.update(DELETE_BY_ID, id);
    }

    private final RowMapper<AccessibleRoomDTO> accessibleRoomMapper = (rs, rowNum) -> {
        Double nearestRampMeters = rs.getObject("nearest_ramp_m", Double.class);
        boolean accessible = nearestRampMeters != null && nearestRampMeters <= ACCESSIBLE_RADIUS_METERS;
        return new AccessibleRoomDTO(
                rs.getLong("room_id"),
                rs.getString("room_code"),
                rs.getString("room_name"),
                rs.getObject("building_id", Long.class),
                accessible,
                nearestRampMeters
        );
    };

    public List<AccessibleRoomDTO> findAccessibleRooms(Long buildingId) {
        return jdbcTemplate.query(FIND_ACCESSIBLE_ROOMS, accessibleRoomMapper, buildingId, buildingId);
    }
}