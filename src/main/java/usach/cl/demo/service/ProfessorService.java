package usach.cl.demo.service;

import usach.cl.demo.entity.Professor;
import usach.cl.demo.entity.Student;
import usach.cl.demo.entity.User;
import usach.cl.demo.model.*;
import usach.cl.demo.repository.ProfessorRepository;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProfessorService {
    private final ProfessorRepository professorRepository;
    private final UserService userService;

    public ProfessorService(ProfessorRepository professorRepository, UserService userService) {
        this.professorRepository = professorRepository;
        this.userService = userService;
    }

    @Transactional
    public Professor create(@Nonnull ProfessorDto dto) throws Exception{
        User user = userService.create(
                new UserDto(dto.name(), dto.email(), dto.password(), Role.PROFESSOR)
        );
        Professor professor = new Professor(user, dto.department());
        return professorRepository.save(professor);
    }

    public Professor getById(int userId) {
        return professorRepository.findByUserId(userId);
    }

    public List<Professor> getAll() {
        return professorRepository.findAll();
    }

    @Transactional
    public Professor update(int userId, @Nonnull ProfessorDto dto) {
        userService.updateUser(userId, dto.name(), dto.email());
        professorRepository.updateProfessor(userId, dto.department());
        return getById(userId);
    }

    @Transactional
    public void delete(int userId) {
        professorRepository.deleteByUserId(userId);
        userService.deleteUser(userId);
    }
}