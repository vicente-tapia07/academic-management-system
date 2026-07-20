package usach.cl.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Respuesta de GET /api/rooms/accessible?buildingId=
// Cada sala del edificio, marcada como accesible si hay una rampa
// (accessibility_poi) a menos de 50 metros.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccessibleRoomDTO {
    private Long roomId;
    private String roomCode;
    private String roomName;
    private Long buildingId;
    private boolean accessible;
    private Double nearestRampMeters; // null si no hay ninguna rampa registrada
}
