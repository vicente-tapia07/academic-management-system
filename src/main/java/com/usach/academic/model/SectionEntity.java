package com.usach.academic.model;

// entidad que representa una seccion de una asignatura en un semestre
public class SectionEntity {

    private Long id;
    private Long subjectId;
    private Long professorId;
    private Long semesterId;
    private int totalSeats;
    private int availableSeats;

    // retorna true si hay cupos disponibles en la seccion
    public boolean isAvailable() {
        return this.availableSeats > 0;
    }

    // reduce en 1 el cupo disponible al inscribir un alumno
    public void reduceSeats() {
        if (this.availableSeats > 0) {
            this.availableSeats--;
        }
    }

    // libera un cupo cuando un alumno se desinscribe
    public void releaseSeats() {
        if (this.availableSeats < this.totalSeats) {
            this.availableSeats++;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }

    public Long getProfessorId() { return professorId; }
    public void setProfessorId(Long professorId) { this.professorId = professorId; }

    public Long getSemesterId() { return semesterId; }
    public void setSemesterId(Long semesterId) { this.semesterId = semesterId; }

    public int getTotalSeats() { return totalSeats; }
    public void setTotalSeats(int totalSeats) { this.totalSeats = totalSeats; }

    public int getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }
}