package usach.cl.demo.service;

import usach.cl.demo.model.SectionEntity;
import usach.cl.demo.repository.SectionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

// Servicio con la logica de negocio para secciones
@Service
public class SectionService {

    private final SectionRepository sectionRepository;

    public SectionService(SectionRepository sectionRepository) {
        this.sectionRepository = sectionRepository;
    }

    // retorna todas las secciones
    public List<SectionEntity> findAll() {
        return sectionRepository.findAll();
    }

    // busca una seccion por id, lanza excepcion si no existe
    public SectionEntity findById(Long id) {
        Optional<SectionEntity> section = sectionRepository.findById(id);
        return section.orElseThrow(() -> new RuntimeException("Section not found with id: " + id));
    }

    // crea una nueva seccion validando cupos
    public SectionEntity save(SectionEntity section) {
        if (section.getTotalSeats() <= 0) {
            throw new RuntimeException("Total spots must be greater than 0");
        }
        // los cupos disponibles empiezan iguales a los totales
        section.setAvailableSeats(section.getTotalSeats());
        return sectionRepository.save(section);
    }
}