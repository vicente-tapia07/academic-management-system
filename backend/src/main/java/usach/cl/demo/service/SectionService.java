package usach.cl.demo.service;

import usach.cl.demo.model.SectionEntity;
import usach.cl.demo.model.RoomEntity;
import usach.cl.demo.repository.SectionRepository;
import usach.cl.demo.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SectionService {

    private final SectionRepository sectionRepository;
    private final RoomRepository roomRepository; // Inyectado para validar capacidad

    public SectionService(SectionRepository sectionRepository, RoomRepository roomRepository) {
        this.sectionRepository = sectionRepository;
        this.roomRepository = roomRepository;
    }

    public List<SectionEntity> findAll() {
        return sectionRepository.findAll();
    }

    public SectionEntity findById(Long id) {
        return sectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Section not found with id: " + id));
    }

    private void validateRoomCapacity(SectionEntity section) {
        RoomEntity room = roomRepository.findById(section.getRoomId())
                .orElseThrow(() -> new RuntimeException("Sala no encontrada"));
        if (section.getTotalSeats() > room.getCapacity()) {
            throw new RuntimeException("Los cupos (" + section.getTotalSeats() + ") superan la capacidad de la sala (" + room.getCapacity() + ")");
        }
    }

    public SectionEntity save(SectionEntity section) {
        if (section.getTotalSeats() <= 0)
            throw new RuntimeException("Total spots must be greater than 0");
        
        validateRoomCapacity(section); // Validación agregada
        
        section.setAvailableSeats(section.getTotalSeats());
        return sectionRepository.save(section);
    }

    public SectionEntity update(Long id, SectionEntity section) {
        findById(id); 
        validateRoomCapacity(section); // Validación agregada
        section.setId(id);
        return sectionRepository.update(section);
    }

    public int deleteById(Long id) {
        findById(id); 
        return sectionRepository.deleteById(id);
    }

    public List<SectionEntity> findByStudentId(Long studentId) {
        return sectionRepository.findByStudentId(studentId);
    }

    public List<SectionEntity> findByProfessorIdAndActiveSemester(Long professorId) {
        return sectionRepository.findByProfessorIdAndActiveSemester(professorId);
    }

    public List<SectionEntity> findByProfessorId(Long professorId) {
        return sectionRepository.findByProfessorId(professorId);
    }
}