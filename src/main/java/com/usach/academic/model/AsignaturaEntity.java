
package com.usach.academic.model;

// Entidad que representa una asignatura dentro de una carrera
public class AsignaturaEntity {

    private Long id;
    private String codigo;
    private String nombre;
    private int creditos;
    private Long carreraId;

    // retorna true si la asignatura tiene creditos validos
    public boolean isActiva() {
        return this.creditos > 0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public int getCreditos() { return creditos; }
    public void setCreditos(int creditos) { this.creditos = creditos; }

    public Long getCarreraId() { return carreraId; }
    public void setCarreraId(Long carreraId) { this.carreraId = carreraId; }
}