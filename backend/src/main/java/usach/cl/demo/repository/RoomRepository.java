package usach.cl.demo.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import usach.cl.demo.model.RoomEntity;

import java.util.List;
import java.util.Optional;

@Repository
public class RoomRepository {

    private static final String FIND_ALL =
            "SELECT id, building_id, code, name, capacity, ST_AsGeoJSON(geom) AS geom_json FROM room";

    private static final String FIND_BY_ID =
            "SELECT id, building_id, code, name, capacity, ST_AsGeoJSON(geom) AS geom_json FROM room WHERE id = ?";

    private static final String FIND_BY_BUILDING_ID =
            "SELECT id, building_id, code, name, capacity, ST_AsGeoJSON(geom) AS geom_json FROM room WHERE building_id = ?";

    private static final String INSERT =
            "INSERT INTO room (building_id, code, name, capacity, geom) " +
                    "VALUES (?, ?, ?, ?, ST_GeomFromGeoJSON(?))";

    private static final String UPDATE =
            "UPDATE room SET building_id = ?, code = ?, name = ?, capacity = ?, geom = ST_GeomFromGeoJSON(?) WHERE id = ?";

    private static final String DELETE_BY_ID =
            "DELETE FROM room WHERE id = ?";

    private final JdbcTemplate jdbcTemplate;

    public RoomRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<RoomEntity> roomMapper = (rs, rowNum) -> {
        RoomEntity r = new RoomEntity();
        r.setId(rs.getLong("id"));
        r.setBuildingId(rs.getLong("building_id"));
        r.setCode(rs.getString("code"));
        r.setName(rs.getString("name"));
        r.setCapacity(rs.getInt("capacity"));
        r.setGeomGeoJson(rs.getString("geom_json"));
        return r;
    };

    public List<RoomEntity> findAll() {
        return jdbcTemplate.query(FIND_ALL, roomMapper);
    }

    public Optional<RoomEntity> findById(Long id) {
        List<RoomEntity> result = jdbcTemplate.query(FIND_BY_ID, roomMapper, id);
        return result.stream().findFirst();
    }

    public List<RoomEntity> findByBuildingId(Long buildingId) {
        return jdbcTemplate.query(FIND_BY_BUILDING_ID, roomMapper, buildingId);
    }

    public int save(RoomEntity room) {
        return jdbcTemplate.update(INSERT,
                room.getBuildingId(),
                room.getCode(),
                room.getName(),
                room.getCapacity(),
                room.getGeomGeoJson()
        );
    }

    public int update(RoomEntity room) {
        return jdbcTemplate.update(UPDATE,
                room.getBuildingId(),
                room.getCode(),
                room.getName(),
                room.getCapacity(),
                room.getGeomGeoJson(),
                room.getId()
        );
    }

    public int deleteById(Long id) {
        return jdbcTemplate.update(DELETE_BY_ID, id);
    }
}