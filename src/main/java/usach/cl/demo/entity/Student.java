package usach.cl.demo.entity;

import usach.cl.demo.model.Role;
import jakarta.annotation.Nonnull;

public class Student extends User {
    private final String studentId;   // Rut/Matrícula
    private final String program;     // Carrera

    public Student(@Nonnull User user, @Nonnull String studentId, @Nonnull String program) {
        super(user.getId(), user.getName(), user.getEmail(), user.getPassword(), user.getRole());
        this.studentId = studentId;
        this.program = program;
    }

    public Student(int id, @Nonnull String name, @Nonnull String email,
                   @Nonnull String password, @Nonnull String studentId, @Nonnull String program) {
        super(id, name, email, password, Role.STUDENT);
        this.studentId = studentId;
        this.program = program;
    }

    public String getStudentId() { return studentId; }
    public String getProgram() { return program; }
}