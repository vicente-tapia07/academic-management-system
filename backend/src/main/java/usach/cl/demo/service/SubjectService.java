package usach.cl.demo.service;

import usach.cl.demo.model.CareerEntity;
import usach.cl.demo.model.SubjectEntity;
import usach.cl.demo.repository.MongoCareerRepository;
import usach.cl.demo.repository.MongoSubjectRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubjectService {

    private final MongoSubjectRepository subjectRepository;
    private final MongoCareerRepository careerRepository;

    public SubjectService(MongoSubjectRepository subjectRepository,
                          MongoCareerRepository careerRepository) {
        this.subjectRepository = subjectRepository;
        this.careerRepository = careerRepository;
    }

    public List<SubjectEntity> findAll() {
        return subjectRepository.findAll();
    }

    public SubjectEntity findById(String id) {
        SubjectEntity subject = subjectRepository.findById(id);
        if (subject == null) throw new RuntimeException("Subject not found with id: " + id);
        return subject;
    }

    public List<SubjectEntity> findByCareerId(String careerId) {
        CareerEntity career = careerRepository.findById(careerId);
        if (career == null) return List.of();
        return subjectRepository.findByCareerCode(career.getCode());
    }

    public List<SubjectEntity> findByCareerIdAndActive(String careerId) {
        CareerEntity career = careerRepository.findById(careerId);
        if (career == null) return List.of();
        return subjectRepository.findByCareerCodeAndActive(career.getCode());
    }

    public List<SubjectEntity> findByCareerCode(String careerCode) {
        return subjectRepository.findByCareerCode(careerCode);
    }

    public List<SubjectEntity> search(String query) {
        return subjectRepository.search(query);
    }

    public SubjectEntity save(SubjectEntity subject) {
        validate(subject);
        return subjectRepository.save(subject);
    }

    public SubjectEntity update(String id, SubjectEntity subject) {
        findById(id);
        validate(subject);
        subject.setId(id);
        return subjectRepository.save(subject);
    }

    public void delete(String id) {
        findById(id);
        subjectRepository.deleteById(id);
    }

    private void validate(SubjectEntity subject) {
        if (subject == null || subject.getCode() == null || subject.getCode().isBlank() ||
                subject.getName() == null || subject.getName().isBlank() || subject.getCareerCode() == null) {
            throw new IllegalArgumentException("Código, nombre y carrera son obligatorios");
        }
        if (subject.getCredits() <= 0) {
            throw new IllegalArgumentException("Los créditos deben ser mayores que 0");
        }
        CareerEntity career = careerRepository.findByCode(subject.getCareerCode());
        if (career == null) {
            throw new IllegalArgumentException("La carrera no existe: " + subject.getCareerCode());
        }
    }
}
