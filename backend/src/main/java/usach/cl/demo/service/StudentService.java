package usach.cl.demo.service;

import org.springframework.stereotype.Service;
import usach.cl.demo.dto.SubjectStatusDTO;
import usach.cl.demo.model.StudentEntity;
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
    public List<StudentEntity> findAll() {
        return studentRepository.findAll();
    }

    // retorna un estudiante por su ID
    public Optional<StudentEntity> findById(Long id) {
        return studentRepository.findById(id);
    }

    // crea un nuevo estudiante
    public int save(StudentEntity studentEntity) {
        return studentRepository.save(studentEntity);
    }

    // actualiza los datos de un estudiante existente
    public int update(StudentEntity studentEntity) {
        return studentRepository.update(studentEntity);
    }

    // elimina un estudiante por su ID
    public int deleteById(Long id) {
        return studentRepository.deleteById(id);
    }


    // Retorna la malla curricular de un estudiante
    public List<SubjectStatusDTO> findCurriculum(Long studentId) {
        return studentRepository.findCurriculum(studentId);
    }

}