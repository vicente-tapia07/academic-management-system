package usach.cl.demo.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
public class SectionEntity {

    private String id;
    private String subjectId;
    private Long professorId;          // usuario id del profesor
    private String professorName;
    private String semesterId;
    private int totalSeats;
    private int availableSeats;
    private SectionRoom room;
    private Integer dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;             // OPEN, CLOSED, CANCELLED

    public boolean isAvailable() {
        return "OPEN".equals(status) && availableSeats > 0;
    }

    public void reduceSeats() {
        if (availableSeats > 0) {
            availableSeats--;
        }
    }

    public void releaseSeats() {
        availableSeats++;
    }

    public String getDayName() {
        if (dayOfWeek == null) return "";
        return switch (dayOfWeek) {
            case 0 -> "Dom";
            case 1 -> "Lun";
            case 2 -> "Mar";
            case 3 -> "Mié";
            case 4 -> "Jue";
            case 5 -> "Vie";
            case 6 -> "Sáb";
            default -> "";
        };
    }

    public String getScheduleLabel() {
        return getDayName() + " " + (startTime != null ? startTime : "") + "-" + (endTime != null ? endTime : "");
    }
}
