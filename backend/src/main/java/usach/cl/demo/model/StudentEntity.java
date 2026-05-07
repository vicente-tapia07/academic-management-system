package usach.cl.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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