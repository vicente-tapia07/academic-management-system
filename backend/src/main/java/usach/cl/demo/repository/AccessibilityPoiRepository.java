package usach.cl.demo.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import usach.cl.demo.model.AccessibilityPoiEntity;

import java.util.List;
import java.util.Optional;

@Repository
public class AccessibilityPoiRepository {

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
}
