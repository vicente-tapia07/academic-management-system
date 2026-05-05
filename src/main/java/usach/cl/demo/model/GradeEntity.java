package usach.cl.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("grade")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradeEntity {
    @Id
    private Long id;
    private Long enrollmentId;
    private Double value;
    private LocalDate entryDate;
}