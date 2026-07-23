package usach.cl.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import usach.cl.demo.dto.DensityHeatmapDTO;
import usach.cl.demo.dto.DistrictFailureDTO;
import usach.cl.demo.service.GeoReportService;

import java.util.List;
import java.util.Map;

/**
 * ReportController — expone los endpoints HTTP de reportes geoespaciales.
 *
 * Rutas base: /api/reports
 *
 * Todas las rutas requieren autenticación (cualquier rol) por la regla
 * .anyRequest().authenticated() del SecurityConfig.
 *
 * El Controller NO contiene lógica de negocio: solo recibe la petición,
 * llama al Service y devuelve la respuesta HTTP.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final GeoReportService geoReportService;

    public ReportController(GeoReportService geoReportService) {
        this.geoReportService = geoReportService;
    }

    /**
     * GET /api/reports/density-heatmap
     *
     * Devuelve la densidad estudiantil por edificio.
     * Cada elemento contiene:
     *  - buildingId, buildingCode, buildingName
     *  - geomJson: la geometría del polígono del edificio en formato GeoJSON string
     *  - studentCount: cantidad de estudiantes con inscripciones ACTIVE en ese edificio
     *
     * Ejemplo de respuesta:
     * [
     *   {
     *     "buildingId": 1,
     *     "buildingCode": "FING",
     *     "buildingName": "Facultad de Ingeniería",
     *     "geomJson": "{\"type\":\"Polygon\",\"coordinates\":[[...]]}",
     *     "studentCount": 3
     *   }
     * ]
     */
    @GetMapping("/density-heatmap")
    public ResponseEntity<List<DensityHeatmapDTO>> getDensityHeatmap() {
        List<DensityHeatmapDTO> result = geoReportService.getStudentDensityByBuilding();
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/reports/failure-by-district
     *
     * Devuelve la tasa de reprobación por distrito de vivienda y asignatura.
     * Cada elemento contiene:
     *  - districtId, districtName
     *  - geomJson: la geometría del polígono del distrito en formato GeoJSON string
     *  - subjectId, subjectCode, subjectName
     *  - totalGrades: total de notas registradas
     *  - failedGrades: notas menores a 4.0
     *  - failurePercentage: porcentaje de reprobación (0.0 - 100.0)
     *
     * Ejemplo de una fila de respuesta:
     * {
     *   "districtId": 1,
     *   "districtName": "Estación Central Norte",
     *   "geomJson": "{\"type\":\"Polygon\",\"coordinates\":[[...]]}",
     *   "subjectId": 1,
     *   "subjectCode": "CAL1",
     *   "subjectName": "Cálculo 1",
     *   "totalGrades": 3,
     *   "failedGrades": 1,
     *   "failurePercentage": 33.33
     * }
     */
    @GetMapping("/failure-by-district")
    public ResponseEntity<List<DistrictFailureDTO>> getFailureByDistrict() {
        List<DistrictFailureDTO> result = geoReportService.getFailureRateByDistrict();
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/reports/refresh
     *
     * Refresca manualmente ambas vistas materializadas sin devolver datos.
     * Útil para administradores que quieran forzar el refresco después de
     * cargar datos masivamente.
     *
     * Devuelve 200 OK con un mensaje de confirmación.
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshViews() {
        geoReportService.refreshViews();
        return ResponseEntity.ok(Map.of("message", "Vistas materializadas refrescadas correctamente"));
    }
}