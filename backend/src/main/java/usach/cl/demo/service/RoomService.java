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
        validate(room);
        return roomRepository.save(room);
    }

    public int update(RoomEntity room) {
        validate(room);
        return roomRepository.update(room);
    }

    public int deleteById(Long id) {
        return roomRepository.deleteById(id);
    }

    private void validate(RoomEntity room) {
        if (room == null || room.getBuildingId() == null || isBlank(room.getCode()) ||
                isBlank(room.getName()) || isBlank(room.getGeomGeoJson())) {
            throw new IllegalArgumentException("Edificio, código, nombre y geometría GeoJSON son obligatorios");
        }
        if (room.getCapacity() == null || room.getCapacity() <= 0) {
            throw new IllegalArgumentException("La capacidad debe ser mayor que 0");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
