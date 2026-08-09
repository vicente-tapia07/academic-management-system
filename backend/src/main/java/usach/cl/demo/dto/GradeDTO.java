package usach.cl.demo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradeDTO {
    private String gradeId;
    private Double value;
    private LocalDate entryDate;
    private String subjectId;
    private String subjectCode;
    private String subjectName;
    private String semesterId;
    private Integer semesterYear;
    private String semesterPeriod;
}
