package usach.cl.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// Representa una inscripción en MongoDB (colección enrollments)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentEntity {
    private String id;
    private Long studentId;
    private String sectionId;
    private LocalDate enrollmentDate;
    private String status;
}
