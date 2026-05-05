package usach.cl.demo.entity;

import usach.cl.demo.model.Role;
import jakarta.annotation.Nonnull;

public class Professor extends User {
    private final String department;

    public Professor(@Nonnull User user, @Nonnull String department) {
        super(user.getId(), user.getName(), user.getEmail(), user.getPassword(), user.getRole());
        this.department = department;
    }

    public Professor(int id, @Nonnull String name, @Nonnull String email,
                     @Nonnull String password, @Nonnull String department) {
        super(id, name, email, password, Role.PROFESSOR);
        this.department = department;
    }

    public String getDepartment() { return department; }
}