package usach.cl.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    public boolean isAvailable() {
        return this.availableSeats > 0;
    }

    public void reduceSeats() {
        if (this.availableSeats > 0) {
            this.availableSeats--;
        }
    }

    public void releaseSeats() {
        if (this.availableSeats < this.totalSeats) {
            this.availableSeats++;
        }
    }
}