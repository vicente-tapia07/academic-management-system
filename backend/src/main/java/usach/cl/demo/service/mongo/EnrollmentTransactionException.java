package usach.cl.demo.service.mongo;

public class EnrollmentTransactionException extends RuntimeException {

    public enum Reason {
        STUDENT_NOT_FOUND,
        STUDENT_NOT_ACTIVE,
        SECTION_NOT_FOUND,
        SECTION_NOT_OPEN,
        NO_AVAILABLE_SEATS,
        SUBJECT_NOT_FOUND,
        SEMESTER_NOT_FOUND,
        SEMESTER_NOT_ACTIVE,
        PREREQUISITES_NOT_MET,
        ALREADY_ENROLLED,
        SCHEMA_VALIDATION_FAILED,
        DATABASE_ERROR
    }

    private final Reason reason;

    public EnrollmentTransactionException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public EnrollmentTransactionException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
