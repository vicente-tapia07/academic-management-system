package usach.cl.demo.service;

import usach.cl.demo.model.SemesterEntity;
import usach.cl.demo.repository.MongoSemesterRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SemesterService {

    private final MongoSemesterRepository semesterRepository;

    public SemesterService(MongoSemesterRepository semesterRepository) {
        this.semesterRepository = semesterRepository;
    }

    public List<SemesterEntity> findAll() {
        return semesterRepository.findAll();
    }

    public SemesterEntity findById(String id) {
        SemesterEntity semester = semesterRepository.findById(id);
        if (semester == null) throw new RuntimeException("Semester not found with id: " + id);
        return semester;
    }

    public SemesterEntity save(SemesterEntity semester) {
        validate(semester);
        if (semester.getStatus() == null || semester.getStatus().isBlank()) {
            semester.setStatus("PLANNED");
        }
        return semesterRepository.save(semester);
    }

    public SemesterEntity update(String id, SemesterEntity semester) {
        findById(id);
        validate(semester);
        semester.setId(id);
        return semesterRepository.save(semester);
    }

    public void closeSemester(String id) {
        SemesterEntity semester = findById(id);
        if (semester.isClosed()) {
            throw new RuntimeException("Semester is already closed");
        }
        semesterRepository.closeSemester(id);
    }

    private void validate(SemesterEntity semester) {
        if (semester == null || semester.getPeriod() == null || semester.getPeriod().isBlank() ||
                semester.getStartDate() == null || semester.getEndDate() == null ||
                semester.getGradeStartDate() == null || semester.getGradeEndDate() == null) {
            throw new IllegalArgumentException("Periodo y todas las fechas son obligatorios");
        }
        if (!List.of("1S", "2S").contains(semester.getPeriod())) {
            throw new IllegalArgumentException("El periodo debe ser 1S o 2S");
        }
        if (semester.getYear() < 2000 || semester.getYear() > 2100) {
            throw new IllegalArgumentException("El año debe estar entre 2000 y 2100");
        }
        if (semester.getEndDate().isBefore(semester.getStartDate())) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la inicial");
        }
        if (semester.getGradeStartDate().isBefore(semester.getStartDate()) ||
                semester.getGradeEndDate().isAfter(semester.getEndDate()) ||
                semester.getGradeEndDate().isBefore(semester.getGradeStartDate())) {
            throw new IllegalArgumentException("El periodo de notas debe estar dentro del semestre y en orden");
        }
        if (semester.getStatus() != null &&
                !List.of("PLANNED", "IN_PROGRESS", "CLOSED").contains(semester.getStatus())) {
            throw new IllegalArgumentException("Estado de semestre inválido");
        }
    }
}
