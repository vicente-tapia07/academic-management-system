package usach.cl.demo.service;

import usach.cl.demo.model.SectionEntity;
import usach.cl.demo.repository.SectionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SectionService {

    private final SectionRepository sectionRepository;

    public SectionService(SectionRepository sectionRepository) {
        this.sectionRepository = sectionRepository;
    }

    public List<SectionEntity> findAll() {
        return sectionRepository.findAll();
    }

    public SectionEntity findById(Long id) {
        Optional<SectionEntity> section = sectionRepository.findById(id);
        return section.orElseThrow(() -> new RuntimeException("Section not found with id: " + id));
    }

    public SectionEntity save(SectionEntity section) {
        if (section.getTotalSeats() <= 0) {
            throw new RuntimeException("Total spots must be greater than 0");
        }
        section.setAvailableSeats(section.getTotalSeats());
        return sectionRepository.save(section);
    }
}