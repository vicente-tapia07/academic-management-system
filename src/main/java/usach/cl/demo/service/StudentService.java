package usach.cl.demo.service;

import org.springframework.stereotype.Service;
import usach.cl.demo.model.Student;
import usach.cl.demo.repository.StudentRepository;

import java.util.List;
import java.util.Optional;

@Service
public class StudentService {

    private final StudentRepository studentRepository;

    // spring inyecta el repositorio automaticamente por el constructor
    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    // retorna todos los estudiantes
    public List<Student> findAll() {
        return studentRepository.findAll();
    }

    // retorna un estudiante por su ID
    public Optional<Student> findById(Long id) {
        return studentRepository.findById(id);
    }

    // crea un nuevo estudiante
    public int save(Student student) {
        return studentRepository.save(student);
    }

    // actualiza los datos de un estudiante existente
    public int update(Student student) {
        return studentRepository.update(student);
    }

    // elimina un estudiante por su ID
    public int deleteById(Long id) {
        return studentRepository.deleteById(id);
    }
}