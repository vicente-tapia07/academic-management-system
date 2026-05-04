package usach.cl.demo.repository;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import usach.cl.demo.model.FailureRateDTO;
import java.util.List;

@Repository
public interface FailureRateRepository extends CrudRepository<FailureRateDTO, Long> {

    @Query("SELECT * FROM mv_failure_rate")
    List<FailureRateDTO> getFailureRateReport();

    // Agregamos @Modifying y ejecutamos el comando REFRESH directo
    @Modifying
    @Query("REFRESH MATERIALIZED VIEW mv_failure_rate")
    void refreshView();
}