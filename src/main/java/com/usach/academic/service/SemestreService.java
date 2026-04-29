package com.usach.academic.service;

import com.usach.academic.model.SemestreEntity;
import com.usach.academic.repository.SemestreRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

// Servicio con la logica de negocio para semestres
@Service
public class SemestreService {

    private final SemestreRepository semestreRepository;

    public SemestreService(SemestreRepository semestreRepository) {
        this.semestreRepository = semestreRepository;
    }

    // retorna todos los semestres
    public List<SemestreEntity> findAll() {
        return semestreRepository.findAll();
    }

    // busca un semestre por id, lanza excepcion si no existe
    public SemestreEntity findById(Long id) {
        Optional<SemestreEntity> semestre = semestreRepository.findById(id);
        return semestre.orElseThrow(() -> new RuntimeException("Semestre no encontrado con id: " + id));
    }

    // crea un nuevo semestre validando las fechas
    public SemestreEntity save(SemestreEntity semestre) {
        if (semestre.getFechaInicio() == null || semestre.getFechaFin() == null) {
            throw new RuntimeException("Las fechas de inicio y fin son obligatorias");
        }
        if (semestre.getFechaFin().isBefore(semestre.getFechaInicio())) {
            throw new RuntimeException("La fecha fin no puede ser antes de la fecha inicio");
        }
        if (semestre.getFechaFinNotas().isBefore(semestre.getFechaInicioNotas())) {
            throw new RuntimeException("La fecha fin de notas no puede ser antes de la fecha inicio de notas");
        }
        // si no viene estado se asigna PLANIFICADO por defecto
        if (semestre.getEstado() == null || semestre.getEstado().isBlank()) {
            semestre.setEstado("PLANIFICADO");
        }
        return semestreRepository.save(semestre);
    }

    // actualiza un semestre existente
    public SemestreEntity update(Long id, SemestreEntity semestre) {
        findById(id);
        semestre.setId(id);
        semestreRepository.update(semestre);
        return semestre;
    }

    // llama al SP de cierre de semestre, valida que no este ya cerrado
    public void cerrarSemestre(Long id) {
        SemestreEntity semestre = findById(id);
        if (semestre.isCerrado()) {
            throw new RuntimeException("El semestre ya esta cerrado");
        }
        semestreRepository.cerrarSemestre(id);
    }
}