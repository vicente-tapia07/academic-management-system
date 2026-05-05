package usach.cl.demo.model;

import jakarta.annotation.Nonnull;

public record StudentDto(@Nonnull String name,
                         @Nonnull String email,
                         @Nonnull String password,
                         @Nonnull String studentId,
                         @Nonnull String program) {}