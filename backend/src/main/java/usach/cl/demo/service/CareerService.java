package usach.cl.demo.service;

import usach.cl.demo.model.CareerEntity;
import usach.cl.demo.repository.MongoCareerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CareerService {

    private final MongoCareerRepository careerRepository;

    public CareerService(MongoCareerRepository careerRepository) {
        this.careerRepository = careerRepository;
    }

    public List<CareerEntity> findAll() {
        return careerRepository.findAll();
    }

    public CareerEntity findById(String id) {
        CareerEntity career = careerRepository.findById(id);
        if (career == null) throw new RuntimeException("Career not found with id: " + id);
        return career;
    }

    public CareerEntity save(CareerEntity career) {
        validate(career);
        return careerRepository.save(career);
    }

    public CareerEntity update(String id, CareerEntity career) {
        findById(id);
        validate(career);
        career.setId(id);
        return careerRepository.save(career);
    }

    public void delete(String id) {
        findById(id);
        careerRepository.deleteById(id);
    }

    private void validate(CareerEntity career) {
        if (career == null || career.getCode() == null || career.getCode().isBlank() ||
                career.getName() == null || career.getName().isBlank()) {
            throw new IllegalArgumentException("Código y nombre de carrera son obligatorios");
        }
    }
}
