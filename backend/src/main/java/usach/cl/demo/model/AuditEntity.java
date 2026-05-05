package usach.cl.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("audit")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditEntity {
    @Id
    private Long id;
    private String affectedTable;
    private String operation;
    private String usuarioRut;
    private LocalDateTime operationDate; // TIMESTAMP en BD
    private String oldData;
    private String newData;
}