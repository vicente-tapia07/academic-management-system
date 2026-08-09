package usach.cl.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfessorEntity {
    private Long id;
    private Long usuarioId;
    private String firstName;
    private String lastName;
    private String department;
}
