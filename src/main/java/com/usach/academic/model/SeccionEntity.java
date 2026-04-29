package com.usach.academic.model;

// Entidad que representa una seccion de una asignatura en un semestre
public class SeccionEntity {

    private Long id;
    private Long asignaturaId;
    private Long profesorId;
    private Long semestreId;
    private int cuposTotal;
    private int cuposDisponibles;

    // retorna true si hay cupos disponibles en la seccion
    public boolean isDisponible() {
        return this.cuposDisponibles > 0;
    }

    // reduce en 1 el cupo disponible al inscribir un alumno
    public void reducirCupo() {
        if (this.cuposDisponibles > 0) {
            this.cuposDisponibles--;
        }
    }

    // libera un cupo cuando un alumno se desinscribe
    public void liberarCupo() {
        if (this.cuposDisponibles < this.cuposTotal) {
            this.cuposDisponibles++;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAsignaturaId() { return asignaturaId; }
    public void setAsignaturaId(Long asignaturaId) { this.asignaturaId = asignaturaId; }

    public Long getProfesorId() { return profesorId; }
    public void setProfesorId(Long profesorId) { this.profesorId = profesorId; }

    public Long getSemestreId() { return semestreId; }
    public void setSemestreId(Long semestreId) { this.semestreId = semestreId; }

    public int getCuposTotal() { return cuposTotal; }
    public void setCuposTotal(int cuposTotal) { this.cuposTotal = cuposTotal; }

    public int getCuposDisponibles() { return cuposDisponibles; }
    public void setCuposDisponibles(int cuposDisponibles) { this.cuposDisponibles = cuposDisponibles; }
}