package usach.cl.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradeEntity {
    private String id;
    private String enrollmentId;
    private Double value;
    private LocalDate entryDate;
}
