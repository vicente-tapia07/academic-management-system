package usach.cl.demo.model;

import lombok.Data;

@Data
public class AccessibilityPoiEntity {
    private Long id;
    private String name;
    private Long buildingId;   // nullable: una rampa puede no estar asociada a un edificio
    private String geomGeoJson;
}
