package usach.cl.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DensityHeatmapDTO {
    private Long buildingId;
    private String buildingCode;
    private String buildingName;
    private String geomJson; // Cadena GeoJSON producida por ST_AsGeoJSON
    private Integer studentCount;
}