package usach.cl.demo.service;

import org.springframework.stereotype.Service;
import usach.cl.demo.model.BuildingEntity;
import usach.cl.demo.repository.BuildingRepository;

import java.util.List;
import java.util.Optional;

@Service
public class BuildingService {

    private final BuildingRepository buildingRepository;

    public BuildingService(BuildingRepository buildingRepository) {
        this.buildingRepository = buildingRepository;
    }

    public List<BuildingEntity> findAll() {
        return buildingRepository.findAll();
    }

    public Optional<BuildingEntity> findById(Long id) {
        return buildingRepository.findById(id);
    }

    public int save(BuildingEntity building) {
        return buildingRepository.save(building);
    }

    public int update(BuildingEntity building) {
        return buildingRepository.update(building);
    }

    public int deleteById(Long id) {
        return buildingRepository.deleteById(id);
    }
}