package usach.cl.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// Representa la inscripción de un estudiante en una sección
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentEntity {
    private Long id;
    private Long studentId;        // FK a estudiante
    private Long sectionId;        // FK a seccion
    private LocalDate enrollmentDate;
    private String status;         // ACTIVE, CANCELLED, COMPLETED
}