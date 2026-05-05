package usach.cl.demo.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import usach.cl.demo.model.ProfessorEntity;

@Repository
public interface ProfessorRepository extends CrudRepository<ProfessorEntity, Long> {
    // Hereda métodos como: save(), findById(), findAll(), deleteById()
}