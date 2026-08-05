package usach.cl.demo.service;

import org.springframework.stereotype.Service;
import usach.cl.demo.dto.GradeDTO;
import usach.cl.demo.model.GradeEntity;
import usach.cl.demo.repository.GradeRepository;
import java.util.List;
import java.time.LocalDate;

@Service
public class GradeService {

    private final GradeRepository gradeRepository;

    public GradeService(GradeRepository gradeRepository) {
        this.gradeRepository = gradeRepository;
    }

    public List<GradeEntity> getAll() {
        return gradeRepository.findAll();
    }

    public GradeEntity save(GradeEntity grade) {
        if (grade == null || grade.getEnrollmentId() == null || grade.getValue() == null) {
            throw new IllegalArgumentException("enrollmentId y value son obligatorios");
        }
        if (grade.getValue() < 1.0 || grade.getValue() > 7.0) {
            throw new IllegalArgumentException("La nota debe estar entre 1.0 y 7.0");
        }
        if (grade.getEntryDate() == null) grade.setEntryDate(LocalDate.now());
        return gradeRepository.save(grade);
    }

    public List<GradeDTO> getGradesByStudent(Long studentId) {
        return gradeRepository.findByStudentId(studentId);
    }
}
