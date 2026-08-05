package usach.cl.demo.controller.mongo;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import usach.cl.demo.dto.mongo.GradeDistributionBucket;
import usach.cl.demo.dto.mongo.PassFailRateItem;
import usach.cl.demo.dto.mongo.SubjectSummary;
import usach.cl.demo.service.mongo.CertificateChangeStreamService;
import usach.cl.demo.service.mongo.ReportAggregationService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mongo")
public class MongoReportController {

    private final ReportAggregationService reportAggregationService;
    private final CertificateChangeStreamService certificateChangeStreamService;

    public MongoReportController(
            ReportAggregationService reportAggregationService,
            CertificateChangeStreamService certificateChangeStreamService) {
        this.reportAggregationService = reportAggregationService;
        this.certificateChangeStreamService = certificateChangeStreamService;
    }

    /**
     * Tasa de aprobación/reprobación por asignatura y semestre, más distribución
     * de notas por rango. subjectId y semesterId son opcionales: si se omiten,
     * el reporte agrega todas las combinaciones existentes en grades.
     *
     * GET /api/mongo/reports/pass-fail-rate?subjectId=&semesterId=
     */
    @GetMapping("/reports/pass-fail-rate")
    public ResponseEntity<Map<String, Object>> passFailRate(
            @RequestParam(required = false) String subjectId,
            @RequestParam(required = false) String semesterId) {

        List<PassFailRateItem> rates = reportAggregationService.passFailRate(subjectId, semesterId);
        List<GradeDistributionBucket> distribution =
                reportAggregationService.gradeDistribution(subjectId, semesterId);

        return ResponseEntity.ok(Map.of(
                "bySubjectAndSemester", rates,
                "gradeDistribution", distribution
        ));
    }

    /**
     * Búsqueda de asignaturas por nombre usando el índice de texto del catálogo.
     * GET /api/mongo/subjects/search?q=programacion
     */
    @GetMapping("/subjects/search")
    public ResponseEntity<List<SubjectSummary>> searchSubjects(@RequestParam String q) {
        return ResponseEntity.ok(reportAggregationService.searchSubjects(q));
    }

    /**
     * Certificado de notas materializado del estudiante (colección reactiva
     * certificados_notas, actualizada por Change Streams cada vez que se
     * registra una nueva calificación).
     * GET /api/mongo/certificates/{studentId}
     */
    @GetMapping("/certificates/{studentId}")
    public ResponseEntity<Map<String, Object>> getCertificate(@PathVariable String studentId) {
        Map<String, Object> certificate = certificateChangeStreamService.getCertificate(studentId);
        if (certificate == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(certificate);
    }
}
