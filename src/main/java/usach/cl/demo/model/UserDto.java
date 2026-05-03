package usach.cl.demo.model;

import jakarta.annotation.Nonnull;

public record UserDto(@Nonnull String name,
                      @Nonnull String email,
                      @Nonnull String password,
                      @Nonnull Role role) {}