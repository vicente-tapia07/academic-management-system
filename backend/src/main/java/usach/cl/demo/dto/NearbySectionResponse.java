package usach.cl.demo.dto;

public class NearbySectionResponse {
    private Long sectionId;
    private String sectionCode;
    private Long roomId;
    private String roomName;
    private String buildingName;
    private Double distanceMeters;

    public NearbySectionResponse() {}

    public NearbySectionResponse(Long sectionId, String sectionCode, Long roomId, String roomName, String buildingName, Double distanceMeters) {
        this.sectionId = sectionId;
        this.sectionCode = sectionCode;
        this.roomId = roomId;
        this.roomName = roomName;
        this.buildingName = buildingName;
        this.distanceMeters = distanceMeters;
    }

    public Long getSectionId() { return sectionId; }
    public String getSectionCode() { return sectionCode; }
    public Long getRoomId() { return roomId; }
    public String getRoomName() { return roomName; }
    public String getBuildingName() { return buildingName; }
    public Double getDistanceMeters() { return distanceMeters; }

    public void setSectionId(Long sectionId) { this.sectionId = sectionId; }
    public void setSectionCode(String sectionCode) { this.sectionCode = sectionCode; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public void setBuildingName(String buildingName) { this.buildingName = buildingName; }
    public void setDistanceMeters(Double distanceMeters) { this.distanceMeters = distanceMeters; }
}