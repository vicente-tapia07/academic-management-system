package usach.cl.demo.model;

import lombok.Data;

@Data
public class RoomEntity {
    private Long id;
    private Long buildingId;
    private String code;
    private String name;
    private Integer capacity;
    private String geomGeoJson;
}