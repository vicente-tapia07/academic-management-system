package usach.cl.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import usach.cl.demo.model.*;
import usach.cl.demo.repository.*;
import java.util.List;

@Service
public class ProfessorService {

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private FailureRateRepository failureRateRepository;

    @Autowired
    private AuditRepository auditRepository;

    // 1. Obtener el reporte de reprobación (Tu tarea principal)
    public List<FailureRateDTO> getFailureReport() {
        // Primero refrescamos la vista para tener datos reales
        failureRateRepository.refreshView();
        // Luego devolvemos los datos
        return failureRateRepository.getFailureRateReport();
    }

    // 2. Subir una nota con Auditoría automática
    public GradeEntity saveGrade(GradeEntity grade, String professorRut) {

        // Asignar la fecha actual si no viene en el JSON
        if (grade.getEntryDate() == null) {
            grade.setEntryDate(java.time.LocalDate.now());
        }

        // Guardamos la nota
        GradeEntity savedGrade = gradeRepository.save(grade);

        // Armamos el JSON de los datos nuevos
        String newDataJson = "{\"enrollment_id\": " + grade.getEnrollmentId() + ", \"value\": " + grade.getValue() + "}";

        // REGISTRAMOS EN AUDITORÍA forzando la conversión a JSONB
        auditRepository.logAudit(
                "grade",
                "INSERT",
                professorRut,
                java.time.LocalDateTime.now(),
                null, // oldData (es null porque es un INSERT, no había datos antes)
                newDataJson
        );

        return savedGrade;
    }
}