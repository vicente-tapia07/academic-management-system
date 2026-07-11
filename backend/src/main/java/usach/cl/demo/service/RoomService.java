package usach.cl.demo.service;

import org.springframework.stereotype.Service;
import usach.cl.demo.model.RoomEntity;
import usach.cl.demo.repository.RoomRepository;

import java.util.List;
import java.util.Optional;

@Service
public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public List<RoomEntity> findAll() {
        return roomRepository.findAll();
    }

    public Optional<RoomEntity> findById(Long id) {
        return roomRepository.findById(id);
    }

    public List<RoomEntity> findByBuildingId(Long buildingId) {
        return roomRepository.findByBuildingId(buildingId);
    }

    public int save(RoomEntity room) {
        return roomRepository.save(room);
    }

    public int update(RoomEntity room) {
        return roomRepository.update(room);
    }

    public int deleteById(Long id) {
        return roomRepository.deleteById(id);
    }
}