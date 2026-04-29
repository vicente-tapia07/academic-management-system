package com.usach.academic.model;

import java.time.LocalDate;

// Entidad que representa un semestre academico
public class SemestreEntity {

    private Long id;
    private int anio;
    private String periodo;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private LocalDate fechaInicioNotas;
    private LocalDate fechaFinNotas;
    private String estado; // PLANIFICADO, EN_CURSO, CERRADO

    // retorna true si el semestre esta en curso
    public boolean isActivo() {
        return "EN_CURSO".equals(this.estado);
    }

    // retorna true si hoy esta dentro del periodo de ingreso de notas
    public boolean isEnPeriodoNotas() {
        LocalDate hoy = LocalDate.now();
        return !hoy.isBefore(fechaInicioNotas) && !hoy.isAfter(fechaFinNotas);
    }

    // retorna true si el semestre ya fue cerrado
    public boolean isCerrado() {
        return "CERRADO".equals(this.estado);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public int getAnio() { return anio; }
    public void setAnio(int anio) { this.anio = anio; }

    public String getPeriodo() { return periodo; }
    public void setPeriodo(String periodo) { this.periodo = periodo; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public LocalDate getFechaInicioNotas() { return fechaInicioNotas; }
    public void setFechaInicioNotas(LocalDate fechaInicioNotas) { this.fechaInicioNotas = fechaInicioNotas; }

    public LocalDate getFechaFinNotas() { return fechaFinNotas; }
    public void setFechaFinNotas(LocalDate fechaFinNotas) { this.fechaFinNotas = fechaFinNotas; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}