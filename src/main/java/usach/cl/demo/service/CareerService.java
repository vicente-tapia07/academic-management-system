package usach.cl.demo.service;

import usach.cl.demo.model.CareerEntity;
import usach.cl.demo.repository.CareerRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

// Servicio con la logica de negocio para carreras
@Service
public class CareerService {

    private final CareerRepository careerRepository;

    public CareerService(CareerRepository careerRepository) {
        this.careerRepository = careerRepository;
    }

    // retorna todas las carreras
    public List<CareerEntity> findAll() {
        return careerRepository.findAll();
    }

    // busca una carrera por id, lanza excepcion si no existe
    public CareerEntity findById(Long id) {
        Optional<CareerEntity> career = careerRepository.findById(id);
        return career.orElseThrow(() -> new RuntimeException("Career not found with id: " + id));
    }

    // crea una nueva carrera validando que el codigo no este vacio
    public CareerEntity save(CareerEntity career) {
        if (career.getCode() == null || career.getCode().isBlank()) {
            throw new RuntimeException("Career code cannot be empty");
        }
        if (career.getName() == null || career.getName().isBlank()) {
            throw new RuntimeException("Career name cannot be empty");
        }
        return careerRepository.save(career);
    }

    // actualiza una carrera existente
    public CareerEntity update(Long id, CareerEntity career) {
        // verifica que exista antes de actualizar
        findById(id);
        career.setId(id);
        careerRepository.update(career);
        return career;
    }

    // elimina una carrera por id
    public void delete(Long id) {
        findById(id);
        careerRepository.delete(id);
    }
}