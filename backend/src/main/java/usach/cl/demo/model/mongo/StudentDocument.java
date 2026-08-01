package usach.cl.demo.model.mongo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentDocument {

    public enum AcademicStatus {
        ACTIVE,
        BLOCKED,
        GRADUATED
    }

    private String id;
    private Long userId;
    private String enrollmentNumber;
    private String firstName;
    private String lastName;
    private String careerCode;
    private AcademicStatus academicStatus;
    private Instant createdAt;
    private Instant updatedAt;

    public Document toDocument() {
        Document document = MongoDocumentSupport.documentWithOptionalId(id)
                .append("userId", userId)
                .append("enrollmentNumber", enrollmentNumber)
                .append("firstName", firstName)
                .append("lastName", lastName)
                .append("careerCode", careerCode)
                .append("academicStatus", academicStatus == null ? null : academicStatus.name())
                .append("createdAt", MongoDocumentSupport.date(createdAt));

        MongoDocumentSupport.appendOptionalDate(document, "updatedAt", updatedAt);
        return document;
    }

    public static StudentDocument fromDocument(Document document) {
        StudentDocument student = new StudentDocument();
        student.setId(MongoDocumentSupport.objectIdHex(document, "_id"));
        student.setUserId(MongoDocumentSupport.longValue(document, "userId"));
        student.setEnrollmentNumber(document.getString("enrollmentNumber"));
        student.setFirstName(document.getString("firstName"));
        student.setLastName(document.getString("lastName"));
        student.setCareerCode(document.getString("careerCode"));

        String status = document.getString("academicStatus");
        student.setAcademicStatus(status == null ? null : AcademicStatus.valueOf(status));
        student.setCreatedAt(MongoDocumentSupport.instant(document, "createdAt"));
        student.setUpdatedAt(MongoDocumentSupport.instant(document, "updatedAt"));
        return student;
    }
}
