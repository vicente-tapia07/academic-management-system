package com.usach.academic.service;

import com.usach.academic.model.CarreraEntity;
import com.usach.academic.repository.CarreraRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

// Servicio con la logica de negocio para carreras
@Service
public class CarreraService {

    private final CarreraRepository carreraRepository;

    public CarreraService(CarreraRepository carreraRepository) {
        this.carreraRepository = carreraRepository;
    }

    // retorna todas las carreras
    public List<CarreraEntity> findAll() {
        return carreraRepository.findAll();
    }

    // busca una carrera por id, lanza excepcion si no existe
    public CarreraEntity findById(Long id) {
        Optional<CarreraEntity> carrera = carreraRepository.findById(id);
        return carrera.orElseThrow(() -> new RuntimeException("Carrera no encontrada con id: " + id));
    }

    // crea una nueva carrera validando que el codigo no este vacio
    public CarreraEntity save(CarreraEntity carrera) {
        if (carrera.getCodigo() == null || carrera.getCodigo().isBlank()) {
            throw new RuntimeException("El codigo de la carrera no puede estar vacio");
        }
        if (carrera.getNombre() == null || carrera.getNombre().isBlank()) {
            throw new RuntimeException("El nombre de la carrera no puede estar vacio");
        }
        return carreraRepository.save(carrera);
    }

    // actualiza una carrera existente
    public CarreraEntity update(Long id, CarreraEntity carrera) {
        // verifica que exista antes de actualizar
        findById(id);
        carrera.setId(id);
        carreraRepository.update(carrera);
        return carrera;
    }

    // elimina una carrera por id
    public void delete(Long id) {
        findById(id);
        carreraRepository.delete(id);
    }
}