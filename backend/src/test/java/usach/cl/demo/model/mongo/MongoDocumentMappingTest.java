package usach.cl.demo.model.mongo;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MongoDocumentMappingTest {

    private static final Instant NOW = Instant.parse("2026-08-01T12:00:00Z");

    @Test
    void preservesTheThreeModelsDuringBsonRoundTrip() {
        StudentDocument student = new StudentDocument(
                id(),
                42L,
                "2026001",
                "Ana",
                "Pérez",
                "ICI",
                StudentDocument.AcademicStatus.ACTIVE,
                NOW,
                NOW
        );
        SectionDocument section = new SectionDocument(
                id(),
                id(),
                id(),
                "PROF-1",
                "Carlos Ruiz",
                30,
                12,
                new SectionDocument.Schedule(3, "13:45", "15:05"),
                new SectionDocument.Room("SA-202", "Sala 202", "Ciencias"),
                SectionDocument.SectionStatus.OPEN,
                NOW,
                NOW
        );
        EnrollmentDocument enrollment = new EnrollmentDocument(
                id(),
                id(),
                id(),
                id(),
                id(),
                EnrollmentDocument.EnrollmentStatus.ACTIVE,
                new EnrollmentDocument.BusinessRules(true, true, NOW),
                NOW,
                null,
                null,
                NOW
        );

        assertThat(StudentDocument.fromDocument(student.toDocument())).isEqualTo(student);
        assertThat(SectionDocument.fromDocument(section.toDocument())).isEqualTo(section);
        assertThat(EnrollmentDocument.fromDocument(enrollment.toDocument())).isEqualTo(enrollment);
    }

    @Test
    void acceptsIntOrLongForThePostgresUserReference() {
        Document source = new Document("_id", new ObjectId())
                .append("userId", 7)
                .append("enrollmentNumber", "2026002")
                .append("firstName", "Bruno")
                .append("lastName", "Soto")
                .append("careerCode", "ICI")
                .append("academicStatus", "ACTIVE")
                .append("createdAt", MongoDocumentSupport.date(NOW));

        assertThat(StudentDocument.fromDocument(source).getUserId()).isEqualTo(7L);
    }

    @Test
    void omitsDatesThatDoNotApplyToAnActiveEnrollment() {
        EnrollmentDocument enrollment = new EnrollmentDocument(
                id(), id(), id(), id(), id(),
                EnrollmentDocument.EnrollmentStatus.ACTIVE,
                new EnrollmentDocument.BusinessRules(true, true, NOW),
                NOW, null, null, NOW
        );

        assertThat(enrollment.toDocument())
                .doesNotContainKeys("cancelledAt", "completedAt");
    }

    @Test
    void rejectsAnInvalidObjectIdBeforeCreatingBson() {
        SectionDocument section = new SectionDocument(
                null,
                "id-invalido",
                id(),
                "PROF-1",
                "Carlos Ruiz",
                30,
                30,
                new SectionDocument.Schedule(1, "08:15", "09:35"),
                new SectionDocument.Room("SA-101", "Sala 101", "Ciencias"),
                SectionDocument.SectionStatus.OPEN,
                NOW,
                null
        );

        assertThatThrownBy(section::toDocument)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("subjectId no es un ObjectId válido");
    }

    private String id() {
        return new ObjectId().toHexString();
    }
}
