package usach.cl.demo.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
public class SectionEntity {
    private Long      id;
    private Long      subjectId;
    private Long      professorId;
    private Long      semesterId;
    private int       totalSeats;
    private int       availableSeats;

    // Campos agregados en Lab 2 (Integrante 1)
    private Long      roomId;       // sala donde se dicta la sección
    private Integer   dayOfWeek;    // 0=domingo, 1=lunes, ..., 6=sábado
    private LocalTime startTime;    // hora de inicio
    private LocalTime endTime;      // hora de término

    public boolean isAvailable() {
        return this.availableSeats > 0;
    }

    public void reduceSeats() {
        if (this.availableSeats > 0) this.availableSeats--;
    }

    public void releaseSeats() {
        if (this.availableSeats < this.totalSeats) this.availableSeats++;
    }

    // Devuelve el nombre del día en español
    public String getDayName() {
        if (dayOfWeek == null) return "—";
        String[] days = { "Domingo", "Lunes", "Martes", "Miércoles",
                          "Jueves", "Viernes", "Sábado" };
        return (dayOfWeek >= 0 && dayOfWeek <= 6) ? days[dayOfWeek] : "—";
    }

    // Devuelve el horario formateado como "Lunes 08:00 – 09:30"
    public String getScheduleLabel() {
        if (dayOfWeek == null || startTime == null || endTime == null) return "—";
        return getDayName() + " " + startTime + " – " + endTime;
    }
}
