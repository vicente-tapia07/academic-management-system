package usach.cl.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DistrictFailureDTO {
    private Long districtId;
    private String districtName;
    private String geomJson; // Cadena GeoJSON producida por ST_AsGeoJSON
    private Long subjectId;
    private String subjectCode;
    private String subjectName;
    private Integer totalGrades;
    private Integer failedGrades;
    private Double failurePercentage;
}