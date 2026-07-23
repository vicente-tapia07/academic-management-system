package usach.cl.demo.service;

import org.springframework.stereotype.Service;
import usach.cl.demo.dto.NearestRoomResponseDTO;
import usach.cl.demo.repository.LocationRepository;

import java.util.Optional;

@Service
public class LocationService {

    private final LocationRepository locationRepository;

    public LocationService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    public Optional<NearestRoomResponseDTO> findNearestActiveRoom(Long studentId, Double lat, Double lng) {
        return locationRepository.findNearestActiveRoom(studentId, lat, lng);
    }
}
