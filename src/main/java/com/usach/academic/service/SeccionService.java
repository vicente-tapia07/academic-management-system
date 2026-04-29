package com.usach.academic.service;

import com.usach.academic.model.SeccionEntity;
import com.usach.academic.repository.SeccionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

// Servicio con la logica de negocio para secciones
@Service
public class SeccionService {

    private final SeccionRepository seccionRepository;

    public SeccionService(SeccionRepository seccionRepository) {
        this.seccionRepository = seccionRepository;
    }

    // retorna todas las secciones
    public List<SeccionEntity> findAll() {
        return seccionRepository.findAll();
    }

    // busca una seccion por id, lanza excepcion si no existe
    public SeccionEntity findById(Long id) {
        Optional<SeccionEntity> seccion = seccionRepository.findById(id);
        return seccion.orElseThrow(() -> new RuntimeException("Seccion no encontrada con id: " + id));
    }

    // crea una nueva seccion validando cupos
    public SeccionEntity save(SeccionEntity seccion) {
        if (seccion.getCuposTotal() <= 0) {
            throw new RuntimeException("Los cupos totales deben ser mayores a 0");
        }
        // los cupos disponibles empiezan iguales a los totales
        seccion.setCuposDisponibles(seccion.getCuposTotal());
        return seccionRepository.save(seccion);
    }
}