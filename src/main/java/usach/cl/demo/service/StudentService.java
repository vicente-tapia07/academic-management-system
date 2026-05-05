package usach.cl.demo.service;

import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import usach.cl.demo.dto.SubjectStatusDTO;
import usach.cl.demo.entity.Student;
import usach.cl.demo.entity.User;
import usach.cl.demo.model.StudentDto;
import usach.cl.demo.model.CurriculumDto;
import usach.cl.demo.model.Role;
import usach.cl.demo.model.UserDto;
import usach.cl.demo.model.StudentEntity;
import usach.cl.demo.repository.StudentRepository;

import java.util.List;
import java.util.Optional;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserService userService;

    public StudentService(StudentRepository studentRepository, UserService userService) {
        this.studentRepository = studentRepository;
        this.userService = userService;
    }

    public List<StudentEntity> findAll() {
        return studentRepository.findAll();
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

    @Transactional
    public Student create(@Nonnull StudentDto dto) throws Exception {
        User user = userService.create(
                new UserDto(dto.name(), dto.email(), dto.password(), Role.STUDENT)
        );
        Student student = new Student(user, dto.studentId(), dto.program());
        return studentRepository.save(student);
    }

    public Student getById(int userId) {
        return studentRepository.findByUserId(userId);
    }

    public List<Student> getAll() {
        return studentRepository.findAll();
    }

    @Transactional
    public Student update(int userId, @Nonnull StudentDto dto) {
        userService.updateUser(userId, dto.name(), dto.email());
        studentRepository.updateStudent(userId, dto.studentId(), dto.program());
        return getById(userId);
    }

    @Transactional
    public void delete(int userId) {
        studentRepository.deleteByUserId(userId);
        userService.deleteUser(userId);
    }

    public CurriculumDto getCurriculum(int studentId) {
        throw new UnsupportedOperationException("Coordination pending with Person 3");
    }
}