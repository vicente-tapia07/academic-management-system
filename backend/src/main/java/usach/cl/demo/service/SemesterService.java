package usach.cl.demo.service;

import usach.cl.demo.model.SemesterEntity;
import usach.cl.demo.repository.SemesterRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SemesterService {

    private final SemesterRepository semesterRepository;

    public SemesterService(SemesterRepository semesterRepository) {
        this.semesterRepository = semesterRepository;
    }

    public List<SemesterEntity> findAll() {
        return semesterRepository.findAll();
    }

    public SemesterEntity findById(Long id) {
        Optional<SemesterEntity> semester = semesterRepository.findById(id);
        return semester.orElseThrow(() -> new RuntimeException("Semester not found with id: " + id));
    }

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
        if (semester.getStatus() == null || semester.getStatus().isBlank()) {
            semester.setStatus("PLANNED");
        }
        return semesterRepository.save(semester);
    }

    public SemesterEntity update(Long id, SemesterEntity semester) {
        findById(id);
        semester.setId(id);
        semesterRepository.update(semester);
        return semester;
    }

    public void closeSemester(Long id) {
        SemesterEntity semester = findById(id);
        if (semester.isClosed()) {
            throw new RuntimeException("Semester is already closed");
        }
        semesterRepository.closeSemester(id);
    }
}