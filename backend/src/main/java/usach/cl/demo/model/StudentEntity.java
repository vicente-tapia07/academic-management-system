package usach.cl.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Representa la tabla "student" en la base de datos
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentEntity {
    private Long id;
    private Long usuarioId;
    private String enrollmentNumber;
    private String firstName;
    private String lastName;
    private String academicStatus;
}