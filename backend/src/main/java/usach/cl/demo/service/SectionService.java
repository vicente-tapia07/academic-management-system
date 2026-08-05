package usach.cl.demo.service;
import usach.cl.demo.model.SectionEntity;
import usach.cl.demo.model.RoomEntity;
import usach.cl.demo.model.SemesterEntity;
import usach.cl.demo.repository.SectionRepository;
import usach.cl.demo.repository.RoomRepository;
import usach.cl.demo.repository.SemesterRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.time.LocalTime;
import java.util.Map;

@Service
public class SectionService {
    private static final Map<LocalTime, LocalTime> OFFICIAL_BLOCKS = Map.of(
            LocalTime.of(8, 15), LocalTime.of(9, 35),
            LocalTime.of(9, 50), LocalTime.of(11, 10),
            LocalTime.of(11, 25), LocalTime.of(12, 45),
            LocalTime.of(13, 45), LocalTime.of(15, 5),
            LocalTime.of(15, 20), LocalTime.of(16, 40),
            LocalTime.of(16, 55), LocalTime.of(18, 15),
            LocalTime.of(18, 45), LocalTime.of(20, 5),
            LocalTime.of(20, 5), LocalTime.of(21, 25),
            LocalTime.of(21, 25), LocalTime.of(22, 45)
    );
    private final SectionRepository sectionRepository;
    private final RoomRepository roomRepository;
    private final SemesterRepository semesterRepository;

    public SectionService(SectionRepository sectionRepository, RoomRepository roomRepository,
                          SemesterRepository semesterRepository) {
        this.sectionRepository = sectionRepository;
        this.roomRepository = roomRepository;
        this.semesterRepository = semesterRepository;
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

    private void validateActiveSemester(SectionEntity section) {
        SemesterEntity semester = semesterRepository.findById(section.getSemesterId())
                .orElseThrow(() -> new RuntimeException("Semestre no encontrado"));
        if (!"IN_PROGRESS".equals(semester.getStatus())) {
            throw new RuntimeException("Solo se pueden crear/editar secciones en el semestre en curso.");
        }
    }

    public SectionEntity save(SectionEntity section) {
        validate(section);
        validateActiveSemester(section);
        validateRoomCapacity(section);

        section.setAvailableSeats(section.getTotalSeats());
        return sectionRepository.save(section);
    }

    public SectionEntity update(Long id, SectionEntity section) {
        SectionEntity existing = findById(id);
        validate(section);
        validateActiveSemester(section);
        validateRoomCapacity(section);
        int occupiedSeats = existing.getTotalSeats() - existing.getAvailableSeats();
        if (section.getTotalSeats() < occupiedSeats) {
            throw new IllegalArgumentException(
                    "Los cupos totales no pueden ser menores que los " + occupiedSeats + " cupos ocupados");
        }
        section.setAvailableSeats(section.getTotalSeats() - occupiedSeats);
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

    private void validate(SectionEntity section) {
        if (section == null || section.getSubjectId() == null || section.getProfessorId() == null ||
                section.getSemesterId() == null || section.getRoomId() == null ||
                section.getDayOfWeek() == null || section.getStartTime() == null ||
                section.getEndTime() == null) {
            throw new IllegalArgumentException("Asignatura, profesor, semestre, sala, día y horario son obligatorios");
        }
        if (section.getTotalSeats() <= 0) {
            throw new IllegalArgumentException("Los cupos totales deben ser mayores que 0");
        }
        if (section.getDayOfWeek() < 1 || section.getDayOfWeek() > 6) {
            throw new IllegalArgumentException("El día debe estar entre lunes (1) y sábado (6)");
        }
        LocalTime expectedEnd = OFFICIAL_BLOCKS.get(section.getStartTime());
        if (expectedEnd == null || !expectedEnd.equals(section.getEndTime())) {
            throw new IllegalArgumentException("El horario debe corresponder a un bloque oficial USACH");
        }
    }
}
