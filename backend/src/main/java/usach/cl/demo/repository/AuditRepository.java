package usach.cl.demo.repository;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import usach.cl.demo.model.AuditEntity;
import java.time.LocalDateTime;

@Repository
public interface AuditRepository extends CrudRepository<AuditEntity, Long> {

    @Modifying
    @Query("INSERT INTO audit (affected_table, operation, usuario_rut, operation_date, old_data, new_data) " +
            "VALUES (:table, :op, :rut, :date, CAST(:oldData AS jsonb), CAST(:newData AS jsonb))")
    void logAudit(@Param("table") String table,
                  @Param("op") String op,
                  @Param("rut") String rut,
                  @Param("date") LocalDateTime date,
                  @Param("oldData") String oldData,
                  @Param("newData") String newData);
}