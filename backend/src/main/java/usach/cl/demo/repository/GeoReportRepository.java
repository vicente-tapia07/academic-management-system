package usach.cl.demo.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import usach.cl.demo.dto.DensityHeatmapDTO;
import usach.cl.demo.dto.DistrictFailureDTO;

import java.util.List;

@Repository
public class GeoReportRepository {

    private final JdbcTemplate jdbcTemplate;

    public GeoReportRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Consulta la Vista Materializada de Densidad Estudiantil por Edificio
     */
    public List<DensityHeatmapDTO> getStudentDensityByBuilding() {
        String sql = """
            SELECT building_id, building_code, building_name, geom_json, student_count
            FROM mv_student_density_by_building
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new DensityHeatmapDTO(
            rs.getLong("building_id"),
            rs.getString("building_code"),
            rs.getString("building_name"),
            rs.getString("geom_json"),
            rs.getInt("student_count")
        ));
    }

    /**
     * Consulta la Vista Materializada de Tasa de Reprobación por Distrito de Vivienda
     */
    public List<DistrictFailureDTO> getFailureRateByDistrict() {
        String sql = """
            SELECT district_id, district_name, geom_json, subject_id, subject_code, subject_name,
                   total_grades, failed_grades, failure_percentage
            FROM mv_failure_rate_by_district
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new DistrictFailureDTO(
            rs.getLong("district_id"),
            rs.getString("district_name"),
            rs.getString("geom_json"),
            rs.getLong("subject_id"),
            rs.getString("subject_code"),
            rs.getString("subject_name"),
            rs.getInt("total_grades"),
            rs.getInt("failed_grades"),
            rs.getDouble("failure_percentage")
        ));
    }

    /**
     * Refresca manualmente ambas vistas materializadas
     */
    public void refreshMaterializedViews() {
        jdbcTemplate.execute("REFRESH MATERIALIZED VIEW mv_student_density_by_building");
        jdbcTemplate.execute("REFRESH MATERIALIZED VIEW mv_failure_rate_by_district");
    }
}