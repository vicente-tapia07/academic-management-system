package usach.cl.demo.model;

import lombok.Data;

@Data
public class BuildingEntity {
    private Long id;
    private String code;
    private String name;
    private String geomGeoJson;
    // la geometría ya convertida a texto GeoJSON
}