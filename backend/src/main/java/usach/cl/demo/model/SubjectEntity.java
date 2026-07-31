package usach.cl.demo.model;

public class SubjectEntity {
    private Long id;
    private String code;
    private String name;
    private int credits;
    private Long careerId;
    private boolean active;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getCredits() { return credits; }
    public void setCredits(int credits) { this.credits = credits; }
    public Long getCareerId() { return careerId; }
    public void setCareerId(Long careerId) { this.careerId = careerId; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}