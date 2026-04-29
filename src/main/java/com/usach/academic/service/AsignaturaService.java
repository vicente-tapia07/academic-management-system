package com.usach.academic.service;

import com.usach.academic.model.AsignaturaEntity;
import com.usach.academic.repository.AsignaturaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

// Servicio con la logica de negocio para asignaturas
@Service
public class AsignaturaService {

    private final AsignaturaRepository asignaturaRepository;

    public AsignaturaService(AsignaturaRepository asignaturaRepository) {
        this.asignaturaRepository = asignaturaRepository;
    }

    // retorna todas las asignaturas
    public List<AsignaturaEntity> findAll() {
        return asignaturaRepository.findAll();
    }

    // busca una asignatura por id, lanza excepcion si no existe
    public AsignaturaEntity findById(Long id) {
        Optional<AsignaturaEntity> asignatura = asignaturaRepository.findById(id);
        return asignatura.orElseThrow(() -> new RuntimeException("Asignatura no encontrada con id: " + id));
    }

    // crea una nueva asignatura validando creditos positivos
    public AsignaturaEntity save(AsignaturaEntity asignatura) {
        if (asignatura.getCodigo() == null || asignatura.getCodigo().isBlank()) {
            throw new RuntimeException("El codigo de la asignatura no puede estar vacio");
        }
        if (asignatura.getCreditos() <= 0) {
            throw new RuntimeException("Los creditos deben ser mayores a 0");
        }
        return asignaturaRepository.save(asignatura);
    }

    // actualiza una asignatura existente
    public AsignaturaEntity update(Long id, AsignaturaEntity asignatura) {
        // verifica que exista antes de actualizar
        findById(id);
        asignatura.setId(id);
        asignaturaRepository.update(asignatura);
        return asignatura;
    }

    // elimina una asignatura por id
    public void delete(Long id) {
        findById(id);
        asignaturaRepository.delete(id);
    }
}