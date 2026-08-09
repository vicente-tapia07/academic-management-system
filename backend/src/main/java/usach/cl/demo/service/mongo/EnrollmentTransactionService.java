package usach.cl.demo.service.mongo;

import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import usach.cl.demo.model.mongo.EnrollmentDocument;
import usach.cl.demo.model.mongo.SectionDocument;
import usach.cl.demo.model.mongo.StudentDocument;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gt;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Sorts.ascending;
import static com.mongodb.client.model.Sorts.descending;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.inc;
import static com.mongodb.client.model.Updates.set;
import static usach.cl.demo.service.mongo.EnrollmentTransactionException.Reason.ALREADY_ENROLLED;
import static usach.cl.demo.service.mongo.EnrollmentTransactionException.Reason.DATABASE_ERROR;
import static usach.cl.demo.service.mongo.EnrollmentTransactionException.Reason.NO_AVAILABLE_SEATS;
import static usach.cl.demo.service.mongo.EnrollmentTransactionException.Reason.PREREQUISITES_NOT_MET;
import static usach.cl.demo.service.mongo.EnrollmentTransactionException.Reason.SCHEMA_VALIDATION_FAILED;
import static usach.cl.demo.service.mongo.EnrollmentTransactionException.Reason.SECTION_NOT_FOUND;
import static usach.cl.demo.service.mongo.EnrollmentTransactionException.Reason.SECTION_NOT_OPEN;
import static usach.cl.demo.service.mongo.EnrollmentTransactionException.Reason.SEMESTER_NOT_ACTIVE;
import static usach.cl.demo.service.mongo.EnrollmentTransactionException.Reason.SEMESTER_NOT_FOUND;
import static usach.cl.demo.service.mongo.EnrollmentTransactionException.Reason.STUDENT_NOT_ACTIVE;
import static usach.cl.demo.service.mongo.EnrollmentTransactionException.Reason.STUDENT_NOT_FOUND;
import static usach.cl.demo.service.mongo.EnrollmentTransactionException.Reason.SUBJECT_NOT_FOUND;

@Service
public class EnrollmentTransactionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnrollmentTransactionService.class);
    private static final double PASSING_GRADE = 4.0;
    private static final int DUPLICATE_KEY_ERROR = 11000;
    private static final int DOCUMENT_VALIDATION_ERROR = 121;

    private static final TransactionOptions TRANSACTION_OPTIONS = TransactionOptions.builder()
            .readPreference(ReadPreference.primary())
            .readConcern(ReadConcern.SNAPSHOT)
            .writeConcern(WriteConcern.MAJORITY)
            .build();

    private final MongoClient mongoClient;
    private final MongoCollection<Document> students;
    private final MongoCollection<Document> subjects;
    private final MongoCollection<Document> semesters;
    private final MongoCollection<Document> sections;
    private final MongoCollection<Document> enrollments;
    private final MongoCollection<Document> grades;

    public EnrollmentTransactionService(MongoClient mongoClient, MongoDatabase mongoDatabase) {
        this.mongoClient = mongoClient;
        this.students = mongoDatabase.getCollection("students");
        this.subjects = mongoDatabase.getCollection("subjects");
        this.semesters = mongoDatabase.getCollection("semesters");
        this.sections = mongoDatabase.getCollection("sections");
        this.enrollments = mongoDatabase.getCollection("enrollments");
        this.grades = mongoDatabase.getCollection("grades");
    }

    public Optional<StudentDocument> findStudent(String studentId) {
        ObjectId studentObjectId = requiredObjectId(studentId, "studentId");
        try {
            Document student = students.find(eq("_id", studentObjectId)).first();
            return Optional.ofNullable(student).map(StudentDocument::fromDocument);
        } catch (MongoException exception) {
            throw databaseReadException("consultar el estudiante", exception);
        }
    }

    public Optional<StudentDocument> findStudentByUser(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        try {
            Document student = students.find(eq("userId", userId)).first();
            return Optional.ofNullable(student).map(StudentDocument::fromDocument);
        } catch (MongoException exception) {
            throw databaseReadException("consultar el estudiante por usuario", exception);
        }
    }

    public List<SectionDocument> findAvailableSections(String subjectId, String semesterId) {
        ObjectId subjectObjectId = requiredObjectId(subjectId, "subjectId");
        ObjectId semesterObjectId = requiredObjectId(semesterId, "semesterId");
        try {
            List<Document> documents = sections.find(and(
                            eq("subjectId", subjectObjectId),
                            eq("semesterId", semesterObjectId),
                            eq("status", "OPEN"),
                            gt("availableSeats", 0)
                    ))
                    .sort(ascending("schedule.dayOfWeek", "schedule.startTime"))
                    .into(new ArrayList<>());
            return documents.stream().map(SectionDocument::fromDocument).toList();
        } catch (MongoException exception) {
            throw databaseReadException("consultar las secciones disponibles", exception);
        }
    }

    public List<EnrollmentDocument> findEnrollmentsByStudent(String studentId) {
        ObjectId studentObjectId = requiredObjectId(studentId, "studentId");
        try {
            List<Document> documents = enrollments.find(eq("studentId", studentObjectId))
                    .sort(descending("enrolledAt"))
                    .into(new ArrayList<>());
            return documents.stream().map(EnrollmentDocument::fromDocument).toList();
        } catch (MongoException exception) {
            throw databaseReadException("consultar las inscripciones del estudiante", exception);
        }
    }

    public EnrollmentDocument enroll(String studentId, String sectionId) {
        ObjectId studentObjectId = requiredObjectId(studentId, "studentId");
        ObjectId sectionObjectId = requiredObjectId(sectionId, "sectionId");

        try (ClientSession session = mongoClient.startSession()) {
            EnrollmentDocument enrollment = session.withTransaction(
                    () -> enrollWithinTransaction(session, studentObjectId, sectionObjectId),
                    TRANSACTION_OPTIONS
            );
            LOGGER.info(
                    "Inscripción MongoDB confirmada: enrollmentId={}, studentId={}, sectionId={}",
                    enrollment.getId(),
                    enrollment.getStudentId(),
                    enrollment.getSectionId()
            );
            return enrollment;
        } catch (EnrollmentTransactionException exception) {
            throw exception;
        } catch (MongoWriteException exception) {
            throw translatedWriteException(exception);
        } catch (MongoException exception) {
            throw new EnrollmentTransactionException(
                    DATABASE_ERROR,
                    "No se pudo completar la inscripción por un error de MongoDB",
                    exception
            );
        }
    }

    public boolean cancelAndRestoreSeat(String enrollmentId) {
        ObjectId enrollmentObjectId = requiredObjectId(enrollmentId, "enrollmentId");
        try (ClientSession session = mongoClient.startSession()) {
            Boolean cancelled = session.withTransaction(
                    () -> cancelWithinTransaction(session, enrollmentObjectId),
                    TRANSACTION_OPTIONS
            );
            return Boolean.TRUE.equals(cancelled);
        } catch (MongoException exception) {
            throw new EnrollmentTransactionException(
                    DATABASE_ERROR,
                    "No se pudo cancelar la inscripción por un error de MongoDB",
                    exception
            );
        }
    }

    private Boolean cancelWithinTransaction(ClientSession session, ObjectId enrollmentId) {
        Document enrollment = enrollments.find(session, eq("_id", enrollmentId)).first();
        if (enrollment == null || !"ACTIVE".equals(enrollment.getString("status"))) {
            return false;
        }

        ObjectId sectionId = enrollment.getObjectId("sectionId");
        Instant now = Instant.now();

        enrollments.updateOne(
                session,
                eq("_id", enrollmentId),
                combine(
                        set("status", "CANCELLED"),
                        set("cancelledAt", now),
                        set("updatedAt", now)
                )
        );
        sections.updateOne(
                session,
                eq("_id", sectionId),
                inc("availableSeats", 1)
        );
        return true;
    }

    private EnrollmentDocument enrollWithinTransaction(
            ClientSession session,
            ObjectId studentId,
            ObjectId sectionId) {
        Document student = students.find(session, eq("_id", studentId)).first();
        if (student == null) {
            throw new EnrollmentTransactionException(STUDENT_NOT_FOUND, "El estudiante no existe");
        }
        if (!"ACTIVE".equals(student.getString("academicStatus"))) {
            throw new EnrollmentTransactionException(
                    STUDENT_NOT_ACTIVE,
                    "El estudiante no está habilitado para inscribir asignaturas"
            );
        }

        Document section = sections.find(session, eq("_id", sectionId)).first();
        if (section == null) {
            throw new EnrollmentTransactionException(SECTION_NOT_FOUND, "La sección no existe");
        }
        if (!"OPEN".equals(section.getString("status"))) {
            throw new EnrollmentTransactionException(SECTION_NOT_OPEN, "La sección no está abierta");
        }
        if (section.getInteger("availableSeats", 0) <= 0) {
            throw new EnrollmentTransactionException(NO_AVAILABLE_SEATS, "La sección no tiene cupos disponibles");
        }

        ObjectId subjectId = section.getObjectId("subjectId");
        ObjectId semesterId = section.getObjectId("semesterId");
        Document subject = subjects.find(session, eq("_id", subjectId)).first();
        if (subject == null) {
            throw new EnrollmentTransactionException(SUBJECT_NOT_FOUND, "La asignatura de la sección no existe");
        }

        Document semester = semesters.find(session, eq("_id", semesterId)).first();
        if (semester == null) {
            throw new EnrollmentTransactionException(SEMESTER_NOT_FOUND, "El semestre de la sección no existe");
        }
        if (!"IN_PROGRESS".equals(semester.getString("status"))) {
            throw new EnrollmentTransactionException(
                    SEMESTER_NOT_ACTIVE,
                    "La sección no pertenece a un semestre en curso"
            );
        }

        ensureNotAlreadyEnrolled(session, studentId, sectionId, semesterId);
        ensurePrerequisitesApproved(session, studentId, subject);

        Document reservedSection = sections.findOneAndUpdate(
                session,
                and(
                        eq("_id", sectionId),
                        eq("status", "OPEN"),
                        gt("availableSeats", 0)
                ),
                inc("availableSeats", -1),
                new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        );
        if (reservedSection == null) {
            throw new EnrollmentTransactionException(
                    NO_AVAILABLE_SEATS,
                    "El último cupo fue tomado por otra inscripción"
            );
        }

        Instant now = Instant.now();
        EnrollmentDocument enrollment = new EnrollmentDocument(
                new ObjectId().toHexString(),
                studentId.toHexString(),
                sectionId.toHexString(),
                subjectId.toHexString(),
                semesterId.toHexString(),
                EnrollmentDocument.EnrollmentStatus.ACTIVE,
                new EnrollmentDocument.BusinessRules(true, true, now),
                now,
                null,
                null,
                now
        );

        enrollments.insertOne(session, enrollment.toDocument());
        return enrollment;
    }

    private void ensureNotAlreadyEnrolled(
            ClientSession session,
            ObjectId studentId,
            ObjectId sectionId,
            ObjectId semesterId) {
        Document existingEnrollment = enrollments.find(
                session,
                and(
                        eq("studentId", studentId),
                        eq("sectionId", sectionId),
                        eq("semesterId", semesterId)
                )
        ).first();

        if (existingEnrollment != null) {
            throw new EnrollmentTransactionException(
                    ALREADY_ENROLLED,
                    "El estudiante ya posee una inscripción para esta sección"
            );
        }
    }

    private void ensurePrerequisitesApproved(
            ClientSession session,
            ObjectId studentId,
            Document subject) {
        List<ObjectId> prerequisiteIds = subject.getList("prerequisiteIds", ObjectId.class);
        if (prerequisiteIds == null || prerequisiteIds.isEmpty()) return;

        List<ObjectId> approvedPrerequisites = grades.distinct(
                session,
                "subjectId",
                and(
                        eq("studentId", studentId),
                        in("subjectId", prerequisiteIds),
                        gte("value", PASSING_GRADE)
                ),
                ObjectId.class
        ).into(new ArrayList<>());

        if (approvedPrerequisites.size() != prerequisiteIds.size()) {
            throw new EnrollmentTransactionException(
                    PREREQUISITES_NOT_MET,
                    "El estudiante no tiene aprobados todos los prerrequisitos de la asignatura"
            );
        }
    }

    private ObjectId requiredObjectId(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " es obligatorio");
        }
        if (!ObjectId.isValid(value)) {
            throw new IllegalArgumentException(fieldName + " no es un ObjectId válido");
        }
        return new ObjectId(value);
    }

    private EnrollmentTransactionException translatedWriteException(MongoWriteException exception) {
        int errorCode = exception.getError().getCode();
        if (errorCode == DUPLICATE_KEY_ERROR) {
            return new EnrollmentTransactionException(
                    ALREADY_ENROLLED,
                    "El estudiante ya posee una inscripción para esta sección",
                    exception
            );
        }
        if (errorCode == DOCUMENT_VALIDATION_ERROR) {
            return new EnrollmentTransactionException(
                    SCHEMA_VALIDATION_FAILED,
                    "MongoDB rechazó la inscripción porque no cumple el esquema académico",
                    exception
            );
        }
        return new EnrollmentTransactionException(
                DATABASE_ERROR,
                "MongoDB rechazó la inscripción",
                exception
        );
    }

    private EnrollmentTransactionException databaseReadException(
            String operation,
            MongoException exception) {
        return new EnrollmentTransactionException(
                DATABASE_ERROR,
                "No se pudo " + operation + " en MongoDB",
                exception
        );
    }
}
