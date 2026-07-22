package usach.cl.demo.service;

import org.springframework.stereotype.Service;
import usach.cl.demo.dto.DensityHeatmapDTO;
import usach.cl.demo.dto.DistrictFailureDTO;
import usach.cl.demo.repository.GeoReportRepository;

import java.util.List;

/**
 * GeoReportService — capa de lógica de negocio para los reportes geoespaciales.
 *
 * Responsabilidades:
 *  1. Coordinar el refresco de las vistas materializadas antes de consultar.
 *  2. Delegar la consulta real al GeoReportRepository.
 *  3. Exponer métodos limpios al Controller, sin que este sepa nada de SQL.
 */
@Service
public class GeoReportService {

    private final GeoReportRepository geoReportRepository;

    // Spring inyecta el repository automáticamente por constructor
    public GeoReportService(GeoReportRepository geoReportRepository) {
        this.geoReportRepository = geoReportRepository;
    }

    /**
     * Devuelve la densidad estudiantil activa por edificio.
     *
     * Refresca la vista antes de consultar para garantizar datos actualizados.
     * La vista mv_student_density_by_building cuenta estudiantes con
     * inscripciones ACTIVE en secciones de cada edificio.
     *
     * @return lista de edificios con su geometría GeoJSON y conteo de estudiantes
     */
    public List<DensityHeatmapDTO> getStudentDensityByBuilding() {
        geoReportRepository.refreshMaterializedViews();
        return geoReportRepository.getStudentDensityByBuilding();
    }

    /**
     * Devuelve la tasa de reprobación por distrito de vivienda y asignatura.
     *
     * Refresca la vista antes de consultar. La vista mv_failure_rate_by_district
     * usa ST_Contains para asociar cada estudiante con su distrito según
     * su home_location (punto geoespacial de su dirección).
     *
     * @return lista de filas con distrito, asignatura, total, reprobados y porcentaje
     */
    public List<DistrictFailureDTO> getFailureRateByDistrict() {
        geoReportRepository.refreshMaterializedViews();
        return geoReportRepository.getFailureRateByDistrict();
    }

    /**
     * Refresca manualmente ambas vistas materializadas.
     *
     * Útil para un endpoint administrativo que permita forzar el refresco
     * sin pedir el reporte completo. Por ejemplo, después de cargar
     * masivamente notas o inscripciones.
     */
    public void refreshViews() {
        geoReportRepository.refreshMaterializedViews();
    }
}