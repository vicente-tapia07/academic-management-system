package usach.cl.demo.service;

import org.springframework.stereotype.Service;

import usach.cl.demo.dto.StudentDTO;
import usach.cl.demo.dto.SubjectStatusDTO;
import usach.cl.demo.model.StudentEntity;
import usach.cl.demo.repository.StudentRepository;

import java.util.List;
import java.util.Optional;

@Service
public class StudentService {

    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    public List<StudentEntity> findAll() {
        return studentRepository.findAll();
    }

    public void saveWithUsuario(StudentDTO dto) {
        studentRepository.saveWithUsuario(dto);
    }

    public Optional<StudentEntity> findById(Long id) {
        return studentRepository.findById(id);
    }

    public int save(StudentEntity studentEntity) {
        return studentRepository.save(studentEntity);
    }

    public int update(StudentEntity studentEntity) {
        return studentRepository.update(studentEntity);
    }

    public int deleteById(Long id) {
        return studentRepository.deleteById(id);
    }


    public List<SubjectStatusDTO> findCurriculum(Long studentId) {
        return studentRepository.findCurriculum(studentId);
    }

}