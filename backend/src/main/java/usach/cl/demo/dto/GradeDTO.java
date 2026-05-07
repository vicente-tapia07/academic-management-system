package usach.cl.demo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradeDTO {
    private Long gradeId;
    private Double value;
    private LocalDate entryDate;
    private Long subjectId;
    private String subjectCode;
    private String subjectName;
}