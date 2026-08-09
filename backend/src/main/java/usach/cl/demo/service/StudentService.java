package usach.cl.demo.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import usach.cl.demo.dto.StudentDTO;
import usach.cl.demo.dto.SubjectStatusDTO;
import usach.cl.demo.model.StudentEntity;
import usach.cl.demo.repository.MongoStudentRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class StudentService {

    private final MongoStudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    public StudentService(MongoStudentRepository studentRepository,
                          PasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<StudentEntity> findAll() {
        return studentRepository.findAll();
    }

    public StudentEntity saveWithUsuario(StudentDTO dto) {
        if (dto == null || isBlank(dto.rut()) || isBlank(dto.email()) || isBlank(dto.password()) ||
                isBlank(dto.firstName()) || isBlank(dto.lastName()) || isBlank(dto.enrollmentNumber())) {
            throw new IllegalArgumentException("Todos los datos del estudiante son obligatorios");
        }
        return studentRepository.saveWithUsuario(dto, passwordEncoder.encode(dto.password()));
    }

    public Optional<StudentEntity> findById(Long id) {
        return Optional.ofNullable(studentRepository.findById(id));
    }

    public StudentEntity update(Long id, StudentEntity student) {
        StudentEntity existing = findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
        if (student.getFirstName() == null || student.getFirstName().isBlank() ||
                student.getLastName() == null || student.getLastName().isBlank()) {
            throw new IllegalArgumentException("Nombre y apellido son obligatorios");
        }
        String status = student.getAcademicStatus();
        if (status == null || status.isBlank() || !Set.of("ACTIVE", "BLOCKED", "GRADUATED").contains(status)) {
            throw new IllegalArgumentException("Estado académico inválido");
        }
        existing.setFirstName(student.getFirstName());
        existing.setLastName(student.getLastName());
        existing.setAcademicStatus(status);
        studentRepository.update(existing);
        return existing;
    }

    public void delete(Long id) {
        findById(id).orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
        studentRepository.deleteById(id);
    }

    public List<SubjectStatusDTO> findCurriculum(Long studentId) {
        return studentRepository.findCurriculum(studentId);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
