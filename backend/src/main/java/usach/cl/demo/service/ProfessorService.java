package usach.cl.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import usach.cl.demo.dto.FailureRateDTO;
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

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private SectionRepository sectionRepository;
    
    // Obtener todos los profesores
    public List<ProfessorEntity> getAll() {
        return professorRepository.findAll();
    }


    public ProfessorEntity getById(Long id) {
        return professorRepository.findById(id);
    }

    public List<SectionEntity> getSectionsByProfessorId(Long professorId) {
        return sectionRepository.findByProfessorId(professorId);
    }

    // Crear profesor
    public ProfessorEntity create(usach.cl.demo.dto.ProfessorDTO dto) {
        ProfessorEntity professor = new ProfessorEntity();
        // El id se genera en la BD, usuarioId se asume igual a id por ahora
        professor.setUsuarioId(null); // Debe asignarse correctamente según lógica de usuario
        // Separar nombre completo en firstName y lastName si es posible
        String[] names = dto.name().split(" ", 2);
        professor.setFirstName(names.length > 0 ? names[0] : "");
        professor.setLastName(names.length > 1 ? names[1] : "");
        professor.setDepartment(dto.department());
        // El repositorio espera usuarioId, department, firstName, lastName
        return professorRepository.save(professor);
    }

    // Actualizar profesor
    public ProfessorEntity update(Long id, usach.cl.demo.dto.ProfessorDTO dto) {
        ProfessorEntity existing = professorRepository.findByUserId(id);
        String[] names = dto.name().split(" ", 2);
        String firstName = names.length > 0 ? names[0] : "";
        String lastName = names.length > 1 ? names[1] : "";
        professorRepository.updateProfessor(id, dto.department(), firstName, lastName);
        // Retornar el profesor actualizado
        existing.setFirstName(firstName);
        existing.setLastName(lastName);
        existing.setDepartment(dto.department());
        return existing;
    }

    // Eliminar profesor
    public void delete(Long id) {
        professorRepository.deleteByUserId(id);
    }

    // Obtener el reporte de reprobación
    public List<FailureRateDTO> getFailureReport() {
        // Primero refrescamos la vista para tener datos reales
        failureRateRepository.refreshView();
        // Luego devolvemos los datos
        return failureRateRepository.getFailureRateReport();
    }

    // Subir una nota con Auditoría automática
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