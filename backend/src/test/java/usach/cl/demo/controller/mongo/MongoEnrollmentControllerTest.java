package usach.cl.demo.controller.mongo;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import usach.cl.demo.controller.ApiExceptionHandler;
import usach.cl.demo.model.mongo.EnrollmentDocument;
import usach.cl.demo.model.mongo.SectionDocument;
import usach.cl.demo.model.mongo.StudentDocument;
import usach.cl.demo.service.AuthorizationService;
import usach.cl.demo.service.mongo.EnrollmentTransactionException;
import usach.cl.demo.service.mongo.EnrollmentTransactionService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MongoEnrollmentControllerTest {

    private static final String STUDENT_ID = "66f000000000000000000701";
    private static final String SUBJECT_ID = "66f000000000000000000702";
    private static final String SEMESTER_ID = "66f000000000000000000703";
    private static final String SECTION_ID = "66f000000000000000000704";
    private static final Instant NOW = Instant.parse("2026-08-01T12:00:00Z");

    private EnrollmentTransactionService enrollmentService;
    private AuthorizationService authorizationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        enrollmentService = mock(EnrollmentTransactionService.class);
        authorizationService = mock(AuthorizationService.class);
        MongoEnrollmentController controller = new MongoEnrollmentController(
                enrollmentService,
                authorizationService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void exposesStudentAndAvailableSectionQueries() throws Exception {
        when(enrollmentService.findStudent(STUDENT_ID)).thenReturn(Optional.of(student()));
        when(enrollmentService.findAvailableSections(SUBJECT_ID, SEMESTER_ID))
                .thenReturn(List.of(section()));

        mockMvc.perform(get("/api/mongo/students/{id}", STUDENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(STUDENT_ID))
                .andExpect(jsonPath("$.userId").value(1));

        mockMvc.perform(get("/api/mongo/sections")
                        .param("subjectId", SUBJECT_ID)
                        .param("semesterId", SEMESTER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(SECTION_ID))
                .andExpect(jsonPath("$[0].availableSeats").value(3));

        verify(authorizationService).requireMongoStudentAccess(null, 1L);
    }

    @Test
    void createsEnrollmentAndListsItForTheStudent() throws Exception {
        EnrollmentDocument enrollment = enrollment();
        when(enrollmentService.findStudent(STUDENT_ID)).thenReturn(Optional.of(student()));
        when(enrollmentService.enroll(STUDENT_ID, SECTION_ID)).thenReturn(enrollment);
        when(enrollmentService.findEnrollmentsByStudent(STUDENT_ID))
                .thenReturn(List.of(enrollment));

        mockMvc.perform(post("/api/mongo/enrollments/enroll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentId": "%s",
                                  "sectionId": "%s"
                                }
                                """.formatted(STUDENT_ID, SECTION_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentId").value(STUDENT_ID))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get("/api/mongo/enrollments/student/{id}", STUDENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sectionId").value(SECTION_ID));
    }

    @Test
    void returnsNotFoundWhenStudentDoesNotExist() throws Exception {
        when(enrollmentService.findStudent(STUDENT_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/mongo/students/{id}", STUDENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDENT_NOT_FOUND"));
    }

    @Test
    void translatesPrerequisiteAndDuplicateRejections() throws Exception {
        when(enrollmentService.findStudent(STUDENT_ID)).thenReturn(Optional.of(student()));
        when(enrollmentService.enroll(STUDENT_ID, SECTION_ID))
                .thenThrow(new EnrollmentTransactionException(
                        EnrollmentTransactionException.Reason.PREREQUISITES_NOT_MET,
                        "Falta un prerrequisito"
                ))
                .thenThrow(new EnrollmentTransactionException(
                        EnrollmentTransactionException.Reason.ALREADY_ENROLLED,
                        "Inscripción duplicada"
                ));

        String request = """
                {"studentId":"%s","sectionId":"%s"}
                """.formatted(STUDENT_ID, SECTION_ID);

        mockMvc.perform(post("/api/mongo/enrollments/enroll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("PREREQUISITES_NOT_MET"));

        mockMvc.perform(post("/api/mongo/enrollments/enroll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_ENROLLED"));
    }

    private StudentDocument student() {
        return new StudentDocument(
                STUDENT_ID,
                1L,
                "2026001",
                "Juan",
                "Pérez",
                "ICI",
                StudentDocument.AcademicStatus.ACTIVE,
                NOW,
                null
        );
    }

    private SectionDocument section() {
        return new SectionDocument(
                SECTION_ID,
                SUBJECT_ID,
                SEMESTER_ID,
                "PROF-1",
                "Carlos Ruiz",
                30,
                3,
                new SectionDocument.Schedule(1, "08:15", "09:35"),
                new SectionDocument.Room("SA-101", "Sala 101", "Ciencias"),
                SectionDocument.SectionStatus.OPEN,
                NOW,
                null
        );
    }

    private EnrollmentDocument enrollment() {
        return new EnrollmentDocument(
                new ObjectId().toHexString(),
                STUDENT_ID,
                SECTION_ID,
                SUBJECT_ID,
                SEMESTER_ID,
                EnrollmentDocument.EnrollmentStatus.ACTIVE,
                new EnrollmentDocument.BusinessRules(true, true, NOW),
                NOW,
                null,
                null,
                NOW
        );
    }
}
