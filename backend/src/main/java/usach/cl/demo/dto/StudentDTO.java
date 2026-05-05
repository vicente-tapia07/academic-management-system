package usach.cl.demo.dto;

import jakarta.annotation.Nonnull;

public record StudentDTO(@Nonnull String name,
                         @Nonnull String email,
                         @Nonnull String password,
                         @Nonnull String studentId,
                         @Nonnull String program) {}