package usach.cl.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Student {
    private Long id;
    private Long userId;
    private String enrollment;
    private String firstName;
    private String lastName;
    private String academicStatus;
}