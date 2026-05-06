package usach.cl.demo.service;

import org.springframework.stereotype.Service;
import usach.cl.demo.model.EnrollmentEntity;
import usach.cl.demo.repository.EnrollmentRepository;

import java.util.List;
import java.util.Optional;

@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;

    // Spring inyecta el repositorio automáticamente
    public EnrollmentService(EnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    // Retorna todas las inscripciones
    public List<EnrollmentEntity> findAll() {
        return enrollmentRepository.findAll();
    }

    // Retorna una inscripción por su ID
    public Optional<EnrollmentEntity> findById(Long id) {
        return enrollmentRepository.findById(id);
    }

    // Retorna todas las inscripciones de un estudiante
    public List<EnrollmentEntity> findByStudentId(Long studentId) {
        return enrollmentRepository.findByStudentId(studentId);
    }

    // Crea una nueva inscripción
    public int save(EnrollmentEntity enrollment) {
        return enrollmentRepository.save(enrollment);
    }

    // Actualiza el estado de una inscripción
    public int updateStatus(Long id, String status) {
        return enrollmentRepository.updateStatus(id, status);
    }

    // Elimina una inscripción por su ID
    public int deleteById(Long id) {
        return enrollmentRepository.deleteById(id);
    }

    // Inscribe a un estudiante usando el stored procedure
    public void enrollStudent(Long studentId, Long sectionId) {
        enrollmentRepository.enrollStudent(studentId, sectionId);
    }

    public List<EnrollmentEntity> findBySectionId(Long sectionId) {
        return enrollmentRepository.findBySectionId(sectionId);
    }
}