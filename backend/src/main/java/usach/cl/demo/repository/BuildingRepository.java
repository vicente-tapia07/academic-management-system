package usach.cl.demo.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import usach.cl.demo.model.BuildingEntity;

import java.util.List;
import java.util.Optional;

@Repository
public class BuildingRepository {

    private static final String FIND_ALL =
            "SELECT id, code, name, ST_AsGeoJSON(geom) AS geom_json FROM building";

    private static final String FIND_BY_ID =
            "SELECT id, code, name, ST_AsGeoJSON(geom) AS geom_json FROM building WHERE id = ?";

    private static final String INSERT =
            "INSERT INTO building (code, name, geom) " +
                    "VALUES (?, ?, ST_GeomFromGeoJSON(?))";

    private static final String UPDATE =
            "UPDATE building SET code = ?, name = ?, geom = ST_GeomFromGeoJSON(?) WHERE id = ?";

    private static final String DELETE_BY_ID =
            "DELETE FROM building WHERE id = ?";

    private final JdbcTemplate jdbcTemplate;

    public BuildingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<BuildingEntity> buildingMapper = (rs, rowNum) -> {
        BuildingEntity b = new BuildingEntity();
        b.setId(rs.getLong("id"));
        b.setCode(rs.getString("code"));
        b.setName(rs.getString("name"));
        b.setGeomGeoJson(rs.getString("geom_json"));
        return b;
    };

    public List<BuildingEntity> findAll() {
        return jdbcTemplate.query(FIND_ALL, buildingMapper);
    }

    public Optional<BuildingEntity> findById(Long id) {
        List<BuildingEntity> result = jdbcTemplate.query(FIND_BY_ID, buildingMapper, id);
        return result.stream().findFirst();
    }

    public int save(BuildingEntity building) {
        return jdbcTemplate.update(INSERT,
                building.getCode(),
                building.getName(),
                building.getGeomGeoJson()
        );
    }

    public int update(BuildingEntity building) {
        return jdbcTemplate.update(UPDATE,
                building.getCode(),
                building.getName(),
                building.getGeomGeoJson(),
                building.getId()
        );
    }

    public int deleteById(Long id) {
        return jdbcTemplate.update(DELETE_BY_ID, id);
    }
}