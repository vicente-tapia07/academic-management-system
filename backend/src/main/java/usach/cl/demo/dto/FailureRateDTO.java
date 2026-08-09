package usach.cl.demo.dto;

import lombok.Data;

@Data
public class FailureRateDTO {

    private String subjectId;
    private String subjectCode;
    private String subjectName;
    private String semesterId;
    private Integer semesterYear;
    private String semesterPeriod;
    private Integer totalGrades;
    private Integer failedGrades;
    private Double failurePercentage;
}
