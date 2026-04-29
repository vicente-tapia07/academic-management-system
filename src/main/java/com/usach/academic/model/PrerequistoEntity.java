
package com.usach.academic.model;

// Entidad de enlace entre asignaturas (cual es prerequisito de cual)
public class PrerequistoEntity {

    private Long asignaturaId;
    private Long asignaturaPrerequistoId;

    public Long getAsignaturaId() { return asignaturaId; }
    public void setAsignaturaId(Long asignaturaId) { this.asignaturaId = asignaturaId; }

    public Long getAsignaturaPrerequistoId() { return asignaturaPrerequistoId; }
    public void setAsignaturaPrerequistoId(Long asignaturaPrerequistoId) {
        this.asignaturaPrerequistoId = asignaturaPrerequistoId;
    }
}
