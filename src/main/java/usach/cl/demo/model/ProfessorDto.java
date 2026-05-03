package usach.cl.demo.model;

import jakarta.annotation.Nonnull;

public record ProfessorDto(@Nonnull String name,
                           @Nonnull String email,
                           @Nonnull String password,
                           @Nonnull String department) {}