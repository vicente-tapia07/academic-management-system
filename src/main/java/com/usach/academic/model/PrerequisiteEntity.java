package com.usach.academic.model;

// entidad de enlace entre asignaturas (cual es prerequisito de cual)
public class PrerequisiteEntity {

    private Long subjectId;
    private Long prerequisiteSubjectId;

    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }

    public Long getPrerequisiteSubjectId() { return prerequisiteSubjectId; }
    public void setPrerequisiteSubjectId(Long prerequisiteSubjectId) {
        this.prerequisiteSubjectId = prerequisiteSubjectId;
    }
}