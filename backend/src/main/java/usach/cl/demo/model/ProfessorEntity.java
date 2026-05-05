package usach.cl.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("professor")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfessorEntity {
    @Id
    private Long id;         // BIGSERIAL en BD
    private Long usuarioId;  // Referencia a usuario
    private String firstName;
    private String lastName;
    private String department;
}