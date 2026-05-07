package usach.cl.demo.service;

import org.springframework.stereotype.Service;
import usach.cl.demo.model.EnrollmentEntity;
import usach.cl.demo.repository.EnrollmentRepository;

import java.util.List;
import java.util.Optional;

@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;

    public EnrollmentService(EnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    public List<EnrollmentEntity> findAll() {
        return enrollmentRepository.findAll();
    }

    public Optional<EnrollmentEntity> findById(Long id) {
        return enrollmentRepository.findById(id);
    }

    public List<EnrollmentEntity> findByStudentId(Long studentId) {
        return enrollmentRepository.findByStudentId(studentId);
    }

    public int save(EnrollmentEntity enrollment) {
        return enrollmentRepository.save(enrollment);
    }

    public int updateStatus(Long id, String status) {
        return enrollmentRepository.updateStatus(id, status);
    }

    public int deleteById(Long id) {
        return enrollmentRepository.deleteById(id);
    }

    public boolean cancelEnrollment(Long enrollmentId) {
        Long sectionId = enrollmentRepository.getSectionIdByEnrollmentId(enrollmentId);
        if (sectionId == null) return false;

        enrollmentRepository.restoreSeat(sectionId);

        int deleted = enrollmentRepository.deleteById(enrollmentId);
        return deleted > 0;
    }

    public void enrollStudent(Long studentId, Long sectionId) {
        enrollmentRepository.enrollStudent(studentId, sectionId);
    }

    public List<EnrollmentEntity> findBySectionId(Long sectionId) {
        return enrollmentRepository.findBySectionId(sectionId);
    }
}