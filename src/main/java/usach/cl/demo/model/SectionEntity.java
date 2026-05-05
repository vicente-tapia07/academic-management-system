package usach.cl.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// entidad que representa una seccion de una asignatura en un semestre
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SectionEntity {
    private Long id;
    private Long subjectId;
    private Long professorId;
    private Long semesterId;
    private int totalSeats;
    private int availableSeats;

    // retorna true si hay cupos disponibles en la seccion
    public boolean isAvailable() {
        return this.availableSeats > 0;
    }

    // reduce en 1 el cupo disponible al inscribir un alumno
    public void reduceSeats() {
        if (this.availableSeats > 0) {
            this.availableSeats--;
        }
    }

    // libera un cupo cuando un alumno se desinscribe
    public void releaseSeats() {
        if (this.availableSeats < this.totalSeats) {
            this.availableSeats++;
        }
    }
}