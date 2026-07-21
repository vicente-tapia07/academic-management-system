package usach.cl.demo.dto;

public class NearbySectionResponse {
    private Long sectionId;
    private String sectionCode;
    private Long roomId;
    private String roomName;
    private String buildingName;
    private Double distanceMeters;
    
    public NearbySectionResponse(Long sectionId, String sectionCode, Long roomId, String roomName, String buildingName, Double distanceMeters) {
        this.sectionId = sectionId;
        this.sectionCode = sectionCode;
        this.roomId = roomId;
        this.roomName = roomName;
        this.buildingName = buildingName;
        this.distanceMeters = distanceMeters;
    }
}
