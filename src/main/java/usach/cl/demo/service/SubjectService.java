package usach.cl.demo.service;

import usach.cl.demo.model.SubjectEntity;
import usach.cl.demo.repository.SubjectRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

// Servicio con la logica de negocio para asignaturas
@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;

    public SubjectService(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    // retorna todas las asignaturas
    public List<SubjectEntity> findAll() {
        return subjectRepository.findAll();
    }

    // busca una asignatura por id, lanza excepcion si no existe
    public SubjectEntity findById(Long id) {
        Optional<SubjectEntity> subject = subjectRepository.findById(id);
        return subject.orElseThrow(() -> new RuntimeException("Subject not found with id: " + id));
    }

    // retorna todas las asignaturas de una carrera
    public List<SubjectEntity> findByCareerId(Long careerId) {
        return subjectRepository.findByCareerId(careerId);
    }

    // crea una nueva asignatura validando creditos positivos
    public SubjectEntity save(SubjectEntity subject) {
        if (subject.getCode() == null || subject.getCode().isBlank()) {
            throw new RuntimeException("Subject code cannot be empty");
        }
        if (subject.getCredits() <= 0) {
            throw new RuntimeException("Credits must be greater than 0");
        }
        return subjectRepository.save(subject);
    }

    // actualiza una asignatura existente
    public SubjectEntity update(Long id, SubjectEntity subject) {
        // verifica que exista antes de actualizar
        findById(id);
        subject.setId(id);
        subjectRepository.update(subject);
        return subject;
    }

    // elimina una asignatura por id
    public void delete(Long id) {
        findById(id);
        subjectRepository.delete(id);
    }
}