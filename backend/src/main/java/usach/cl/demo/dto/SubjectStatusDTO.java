package usach.cl.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubjectStatusDTO {
    private Long subjectId;
    private String subjectCode;
    private String subjectName;
    private Integer credits;
    private String status;      // APPROVED, FAILED, ENROLLED, PENDING
    private Double grade;       // null si no tiene nota
}