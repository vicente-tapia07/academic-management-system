package usach.cl.demo.service;

import usach.cl.demo.model.CareerEntity;
import usach.cl.demo.repository.CareerRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CareerService {

    private final CareerRepository careerRepository;

    public CareerService(CareerRepository careerRepository) {
        this.careerRepository = careerRepository;
    }

    public List<CareerEntity> findAll() {
        return careerRepository.findAll();
    }

    public CareerEntity findById(Long id) {
        Optional<CareerEntity> career = careerRepository.findById(id);
        return career.orElseThrow(() -> new RuntimeException("Career not found with id: " + id));
    }

    public CareerEntity save(CareerEntity career) {
        if (career.getCode() == null || career.getCode().isBlank()) {
            throw new RuntimeException("Career code cannot be empty");
        }
        if (career.getName() == null || career.getName().isBlank()) {
            throw new RuntimeException("Career name cannot be empty");
        }
        return careerRepository.save(career);
    }

    public CareerEntity update(Long id, CareerEntity career) {
        findById(id);
        career.setId(id);
        careerRepository.update(career);
        return career;
    }

    public void delete(Long id) {
        findById(id);
        careerRepository.delete(id);
    }
}