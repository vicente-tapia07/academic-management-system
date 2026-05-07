package usach.cl.demo.service;

import org.springframework.stereotype.Service;
import usach.cl.demo.dto.GradeDTO;
import usach.cl.demo.model.GradeEntity;
import usach.cl.demo.repository.GradeRepository;
import java.util.List;

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
        return gradeRepository.save(grade);
    }

    public List<GradeDTO> getGradesByStudent(Long studentId) {
        return gradeRepository.findByStudentId(studentId);
    }
}