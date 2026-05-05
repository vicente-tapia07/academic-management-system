package usach.cl.demo.service;

import usach.cl.demo.model.SemesterEntity;
import usach.cl.demo.repository.SemesterRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

// Servicio con la logica de negocio para semestres
@Service
public class SemesterService {

    private final SemesterRepository semesterRepository;

    public SemesterService(SemesterRepository semesterRepository) {
        this.semesterRepository = semesterRepository;
    }

    // retorna todos los semestres
    public List<SemesterEntity> findAll() {
        return semesterRepository.findAll();
    }

    // busca un semestre por id, lanza excepcion si no existe
    public SemesterEntity findById(Long id) {
        Optional<SemesterEntity> semester = semesterRepository.findById(id);
        return semester.orElseThrow(() -> new RuntimeException("Semester not found with id: " + id));
    }

    // crea un nuevo semestre validando las fechas
    public SemesterEntity save(SemesterEntity semester) {
        if (semester.getStartDate() == null || semester.getEndDate() == null) {
            throw new RuntimeException("Start and end dates are required");
        }
        if (semester.getEndDate().isBefore(semester.getStartDate())) {
            throw new RuntimeException("End date cannot be before start date");
        }
        if (semester.getGradeEndDate().isBefore(semester.getGradeStartDate())) {
            throw new RuntimeException("Grade end date cannot be before grade start date");
        }
        // si no viene estado se asigna PLANIFICADO por defecto
        if (semester.getStatus() == null || semester.getStatus().isBlank()) {
            semester.setStatus("PLANNED");
        }
        return semesterRepository.save(semester);
    }

    // actualiza un semestre existente
    public SemesterEntity update(Long id, SemesterEntity semester) {
        findById(id);
        semester.setId(id);
        semesterRepository.update(semester);
        return semester;
    }

    // llama al SP de cierre de semestre, valida que no este ya cerrado
    public void closeSemester(Long id) {
        SemesterEntity semester = findById(id);
        if (semester.isClosed()) {
            throw new RuntimeException("Semester is already closed");
        }
        semesterRepository.closeSemester(id);
    }
}