package usach.cl.demo.service;

import usach.cl.demo.entity.Student;
import usach.cl.demo.entity.User;
import usach.cl.demo.model.StudentDto;
import usach.cl.demo.model.CurriculumDto;
import usach.cl.demo.model.Role;
import usach.cl.demo.model.UserDto;
import usach.cl.demo.repository.StudentRepository;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StudentService {
    private final StudentRepository studentRepository;
    private final UserService userService;

    public StudentService(StudentRepository studentRepository, UserService userService) {
        this.studentRepository = studentRepository;
        this.userService = userService;
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

    // Placeholder – will be implemented when enrollment data is ready
    public CurriculumDto getCurriculum(int studentId) {
        throw new UnsupportedOperationException("Coordination pending with Person 3");
    }
}