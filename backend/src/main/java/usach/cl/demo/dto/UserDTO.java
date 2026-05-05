package usach.cl.demo.dto;

import jakarta.annotation.Nonnull;
import usach.cl.demo.model.Role;

public record UserDTO(@Nonnull String name,
                      @Nonnull String email,
                      @Nonnull String password,
                      @Nonnull Role role) {}