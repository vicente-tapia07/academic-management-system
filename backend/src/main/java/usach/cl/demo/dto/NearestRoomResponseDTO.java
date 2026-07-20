package usach.cl.demo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NearestRoomResponseDTO {
    private Long roomId;
    private String roomCode;
    private String roomName;
    private Long buildingId;
    private Long sectionId;
    private Long subjectId;
    private String subjectName;
    private Double distanceMeters;
    private String geomGeoJson;
}
