package usach.cl.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Representa una sección de una asignatura en un semestre
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SectionEntity {
    private Long id;
    private Long subjectId;       // asignaturaId
    private Long professorId;     // profesorId
    private Long semesterId;      // semestreId
    private int totalSlots;       // cuposTotal
    private int availableSlots;   // cuposDisponibles

    // Retorna true si hay cupos disponibles en la sección
    public boolean isAvailable() {
        return this.availableSlots > 0;
    }

    // Reduce en 1 el cupo disponible al inscribir un alumno
    public void reduceSlot() {
        if (this.availableSlots > 0) {
            this.availableSlots--;
        }
    }

    // Libera un cupo cuando un alumno se desinscribe
    public void releaseSlot() {
        if (this.availableSlots < this.totalSlots) {
            this.availableSlots++;
        }
    }
}