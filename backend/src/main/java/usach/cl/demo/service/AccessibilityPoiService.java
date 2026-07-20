package usach.cl.demo.service;

import org.springframework.stereotype.Service;
import usach.cl.demo.model.AccessibilityPoiEntity;
import usach.cl.demo.repository.AccessibilityPoiRepository;

import java.util.List;
import java.util.Optional;

@Service
public class AccessibilityPoiService {

    private final AccessibilityPoiRepository accessibilityPoiRepository;

    public AccessibilityPoiService(AccessibilityPoiRepository accessibilityPoiRepository) {
        this.accessibilityPoiRepository = accessibilityPoiRepository;
    }

    public List<AccessibilityPoiEntity> findAll() {
        return accessibilityPoiRepository.findAll();
    }

    public Optional<AccessibilityPoiEntity> findById(Long id) {
        return accessibilityPoiRepository.findById(id);
    }

    public List<AccessibilityPoiEntity> findByBuildingId(Long buildingId) {
        return accessibilityPoiRepository.findByBuildingId(buildingId);
    }

    public int save(AccessibilityPoiEntity poi) {
        return accessibilityPoiRepository.save(poi);
    }

    public int update(AccessibilityPoiEntity poi) {
        return accessibilityPoiRepository.update(poi);
    }

    public int deleteById(Long id) {
        return accessibilityPoiRepository.deleteById(id);
    }
}
