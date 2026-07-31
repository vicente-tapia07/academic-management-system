package usach.cl.demo.service;

import usach.cl.demo.model.SubjectEntity;
import usach.cl.demo.repository.SubjectRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;

    public SubjectService(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    public List<SubjectEntity> findAll() {
        return subjectRepository.findAll();
    }

    public SubjectEntity findById(Long id) {
        Optional<SubjectEntity> subject = subjectRepository.findById(id);
        return subject.orElseThrow(() -> new RuntimeException("Subject not found with id: " + id));
    }

    public List<SubjectEntity> findByCareerId(Long careerId) {
        return subjectRepository.findByCareerId(careerId);
    }

    public SubjectEntity save(SubjectEntity subject) {
        validate(subject);
        return subjectRepository.save(subject);
    }

    public SubjectEntity update(Long id, SubjectEntity subject) {
        findById(id);
        validate(subject);
        subject.setId(id);
        subjectRepository.update(subject);
        return subject;
    }

    public void delete(Long id) {
        findById(id);
        subjectRepository.delete(id);
    }

    private void validate(SubjectEntity subject) {
        if (subject == null || subject.getCode() == null || subject.getCode().isBlank() ||
                subject.getName() == null || subject.getName().isBlank() || subject.getCareerId() == null) {
            throw new IllegalArgumentException("Código, nombre y carrera son obligatorios");
        }
        if (subject.getCredits() <= 0) {
            throw new IllegalArgumentException("Los créditos deben ser mayores que 0");
        }
    }
}
