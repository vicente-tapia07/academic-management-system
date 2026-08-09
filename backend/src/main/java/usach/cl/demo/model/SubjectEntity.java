package usach.cl.demo.model;

import java.util.ArrayList;
import java.util.List;

public class SubjectEntity {
    private String id;
    private String code;
    private String name;
    private int credits;
    private String careerCode;
    private List<String> prerequisiteIds = new ArrayList<>();
    private boolean active;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getCredits() { return credits; }
    public void setCredits(int credits) { this.credits = credits; }
    public String getCareerCode() { return careerCode; }
    public void setCareerCode(String careerCode) { this.careerCode = careerCode; }
    public List<String> getPrerequisiteIds() { return prerequisiteIds; }
    public void setPrerequisiteIds(List<String> prerequisiteIds) { this.prerequisiteIds = prerequisiteIds; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
