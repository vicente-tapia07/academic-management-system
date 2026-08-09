package usach.cl.demo.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubjectStatusDTO {
    private String subjectId;
    private String subjectCode;
    private String subjectName;
    private Integer credits;
    private String status;
    private Double grade;
    private String semesterId;
    private Integer semesterYear;
    private String semesterPeriod;
}
