package usach.cl.demo.model.mongo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentDocument {

    public enum EnrollmentStatus {
        ACTIVE,
        CANCELLED,
        COMPLETED
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BusinessRules {
        private Boolean prerequisitesSatisfied;
        private Boolean seatAvailableAtEnrollment;
        private Instant validatedAt;

        Document toDocument() {
            return new Document("prerequisitesSatisfied", prerequisitesSatisfied)
                    .append("seatAvailableAtEnrollment", seatAvailableAtEnrollment)
                    .append("validatedAt", MongoDocumentSupport.date(validatedAt));
        }

        static BusinessRules fromDocument(Document document) {
            if (document == null) return null;
            return new BusinessRules(
                    document.getBoolean("prerequisitesSatisfied"),
                    document.getBoolean("seatAvailableAtEnrollment"),
                    MongoDocumentSupport.instant(document, "validatedAt")
            );
        }
    }

    private String id;
    private String studentId;
    private String sectionId;
    private String subjectId;
    private String semesterId;
    private EnrollmentStatus status;
    private BusinessRules businessRules;
    private Instant enrolledAt;
    private Instant cancelledAt;
    private Instant completedAt;
    private Instant updatedAt;

    public Document toDocument() {
        Document document = MongoDocumentSupport.documentWithOptionalId(id)
                .append("studentId", MongoDocumentSupport.objectId(studentId, "studentId"))
                .append("sectionId", MongoDocumentSupport.objectId(sectionId, "sectionId"))
                .append("subjectId", MongoDocumentSupport.objectId(subjectId, "subjectId"))
                .append("semesterId", MongoDocumentSupport.objectId(semesterId, "semesterId"))
                .append("status", status == null ? null : status.name())
                .append("businessRules", businessRules == null ? null : businessRules.toDocument())
                .append("enrolledAt", MongoDocumentSupport.date(enrolledAt))
                .append("updatedAt", MongoDocumentSupport.date(updatedAt));

        MongoDocumentSupport.appendOptionalDate(document, "cancelledAt", cancelledAt);
        MongoDocumentSupport.appendOptionalDate(document, "completedAt", completedAt);
        return document;
    }

    public static EnrollmentDocument fromDocument(Document document) {
        EnrollmentDocument enrollment = new EnrollmentDocument();
        enrollment.setId(MongoDocumentSupport.objectIdHex(document, "_id"));
        enrollment.setStudentId(MongoDocumentSupport.objectIdHex(document, "studentId"));
        enrollment.setSectionId(MongoDocumentSupport.objectIdHex(document, "sectionId"));
        enrollment.setSubjectId(MongoDocumentSupport.objectIdHex(document, "subjectId"));
        enrollment.setSemesterId(MongoDocumentSupport.objectIdHex(document, "semesterId"));

        String status = document.getString("status");
        enrollment.setStatus(status == null ? null : EnrollmentStatus.valueOf(status));
        enrollment.setBusinessRules(BusinessRules.fromDocument(document.get("businessRules", Document.class)));
        enrollment.setEnrolledAt(MongoDocumentSupport.instant(document, "enrolledAt"));
        enrollment.setCancelledAt(MongoDocumentSupport.instant(document, "cancelledAt"));
        enrollment.setCompletedAt(MongoDocumentSupport.instant(document, "completedAt"));
        enrollment.setUpdatedAt(MongoDocumentSupport.instant(document, "updatedAt"));
        return enrollment;
    }
}
