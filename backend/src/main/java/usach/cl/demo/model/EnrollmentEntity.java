package usach.cl.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// Representa la tabla "enrollment" en la base de datos
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentEntity {
    private Long id;
    private Long studentId;
    private Long sectionId;
    private LocalDate enrollmentDate;
    private String status;
}