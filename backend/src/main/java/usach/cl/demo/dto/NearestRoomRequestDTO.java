package usach.cl.demo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NearestRoomRequestDTO {
    private Long studentId;
    private Double lat;
    private Double lng;
}
