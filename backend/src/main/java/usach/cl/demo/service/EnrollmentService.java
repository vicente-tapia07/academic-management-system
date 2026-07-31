package usach.cl.demo.service;

import org.springframework.stereotype.Service;

import usach.cl.demo.dto.NearbySectionResponse;
import usach.cl.demo.model.EnrollmentEntity;
import usach.cl.demo.repository.EnrollmentRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public int updateStatus(Long id, String status) {
        if (status == null || !Set.of("ACTIVE", "CANCELLED", "COMPLETED").contains(status)) {
            throw new IllegalArgumentException("Estado inválido. Use ACTIVE, CANCELLED o COMPLETED");
        }
        EnrollmentEntity enrollment = findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada: " + id));
        String current = enrollment.getStatus();
        if (current.equals(status)) return 1;
        if ("COMPLETED".equals(current)) {
            throw new IllegalArgumentException("Una inscripción completada no puede cambiar de estado");
        }
        if ("CANCELLED".equals(status)) {
            return cancelEnrollment(id) ? 1 : 0;
        }
        if ("ACTIVE".equals(status)) {
            if (!"CANCELLED".equals(current)) {
                throw new IllegalArgumentException("Solo una inscripción cancelada puede reactivarse");
            }
            enrollStudent(enrollment.getStudentId(), enrollment.getSectionId());
            return 1;
        }
        if (!enrollmentRepository.hasGrade(id)) {
            throw new IllegalArgumentException("No se puede completar una inscripción sin nota");
        }
        return enrollmentRepository.updateStatus(id, status);
    }

    public boolean cancelEnrollment(Long enrollmentId) {
        return enrollmentRepository.cancelAndRestoreSeat(enrollmentId);
    }

    public void enrollStudent(Long studentId, Long sectionId) {
        if (studentId == null || sectionId == null) {
            throw new IllegalArgumentException("studentId y sectionId son obligatorios");
        }
        enrollmentRepository.enrollStudent(studentId, sectionId);
    }

    public List<EnrollmentEntity> findBySectionId(Long sectionId) {
        return enrollmentRepository.findBySectionId(sectionId);
    }

    public List<NearbySectionResponse> findNearbySections(Long subjectId, Double lat, Double lng) {
        return enrollmentRepository.findNearbySections(subjectId, lat, lng);
    }
}
