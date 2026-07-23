package usach.cl.demo.dto;

import jakarta.annotation.Nonnull;

public record ProfessorDTO(
    @Nonnull String name,
    @Nonnull String email,
    @Nonnull String password,
    @Nonnull String department,
             String rut
) {}
