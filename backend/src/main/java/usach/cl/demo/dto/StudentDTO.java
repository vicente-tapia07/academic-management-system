package usach.cl.demo.dto;

public record StudentDTO(
    String rut,
    String email,
    String password,
    String firstName,
    String lastName,
    String enrollmentNumber
) {}