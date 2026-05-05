package usach.cl.demo.model;

import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class FailureRateDTO {

    @Id
    private Long subjectId;

    private String subjectCode;
    private String subjectName;
    private Integer totalGrades;
    private Integer failedGrades;
    private Double failurePercentage;
}