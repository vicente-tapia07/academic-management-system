package usach.cl.demo.service;

import org.springframework.stereotype.Service;
import usach.cl.demo.model.Student;
import usach.cl.demo.repository.StudentRepository;

import java.util.List;
import java.util.Optional;

@Service
public class StudentService {

    private final StudentRepository studentRepository;

    // Spring inyecta el repositorio automáticamente por el constructor
    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    // Retorna todos los estudiantes
    public List<Student> findAll() {
        return studentRepository.findAll();
    }

    // Retorna un estudiante por su ID
    public Optional<Student> findById(Long id) {
        return studentRepository.findById(id);
    }

    // Crea un nuevo estudiante
    public int save(Student student) {
        return studentRepository.save(student);
    }

    // Actualiza los datos de un estudiante existente
    public int update(Student student) {
        return studentRepository.update(student);
    }

    // Elimina un estudiante por su ID
    public int deleteById(Long id) {
        return studentRepository.deleteById(id);
    }
}