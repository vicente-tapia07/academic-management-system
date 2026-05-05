package usach.cl.demo.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import usach.cl.demo.model.GradeEntity;

@Repository
public interface GradeRepository extends CrudRepository<GradeEntity, Long> {
    // Permite al profesor gestionar las notas de los estudiantes
}