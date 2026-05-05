package usach.cl.demo.model;

import java.time.LocalDate;

// entidad que representa un semestre academico
public class SemesterEntity {

    private Long id;
    private int year;
    private String period;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate gradeStartDate;
    private LocalDate gradeEndDate;
    private String status; // PLANNED, IN_PROGRESS, CLOSED

    // retorna true si el semestre esta en curso
    public boolean isActive() {
        return "IN_PROGRESS".equals(this.status);
    }

    // retorna true si hoy esta dentro del periodo de ingreso de notas
    public boolean isInGradePeriod() {
        LocalDate today = LocalDate.now();
        return !today.isBefore(gradeStartDate) && !today.isAfter(gradeEndDate);
    }

    // retorna true si el semestre ya fue cerrado
    public boolean isClosed() {
        return "CLOSED".equals(this.status);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public LocalDate getGradeStartDate() { return gradeStartDate; }
    public void setGradeStartDate(LocalDate gradeStartDate) { this.gradeStartDate = gradeStartDate; }

    public LocalDate getGradeEndDate() { return gradeEndDate; }
    public void setGradeEndDate(LocalDate gradeEndDate) { this.gradeEndDate = gradeEndDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}