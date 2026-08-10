package usach.cl.demo.service.mongo;

import com.mongodb.client.ChangeStreamIterable;
import com.mongodb.client.MongoChangeStreamCursor;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import jakarta.annotation.PostConstruct;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.mongodb.client.model.Filters.eq;

/**
 * Vista materializada "certificado de notas" (enunciado, tarea 6).
 * <p>
 * Un Change Stream sobre la colecci�n {@code grades} detecta cada alta/modificaci�n de
 * calificaci�n y reconstruye, mediante un pipeline de agregaci�n con {@code $merge}, el
 * documento materializado del estudiante en {@code certificados_notas}.
 */
@Service
public class CertificateChangeStreamService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CertificateChangeStreamService.class);
    private static final Set<String> HANDLED_OPERATIONS = Set.of("insert", "update", "replace");
    private static final int RECONNECT_DELAY_MS = 2_000;
    private static final String CERTIFICATES_COLLECTION = "certificados_notas";

    private final MongoCollection<Document> grades;
    private final MongoCollection<Document> certificates;

    public CertificateChangeStreamService(MongoDatabase mongoDatabase) {
        this.grades = mongoDatabase.getCollection("grades");
        this.certificates = mongoDatabase.getCollection(CERTIFICATES_COLLECTION);
    }

    /**
     * Certificado de notas materializado de un estudiante, listo para exponer por API.
     * Convierte los tipos BSON (ObjectId, Date) a tipos JSON-friendly.
     */
    public Map<String, Object> getCertificate(String studentId) {
        if (!ObjectId.isValid(studentId)) {
            throw new IllegalArgumentException("studentId no es un ObjectId válido");
        }
        Document document = certificates.find(eq("_id", studentId)).first();
        if (document == null) {
            return null;
        }
        return toJsonSafeMap(document);
    }

    @SuppressWarnings("unchecked")
    private Object toJsonSafe(Object value) {
        if (value instanceof Document document) {
            return toJsonSafeMap(document);
        }
        if (value instanceof List<?> list) {
            List<Object> converted = new ArrayList<>(list.size());
            for (Object item : list) {
                converted.add(toJsonSafe(item));
            }
            return converted;
        }
        if (value instanceof ObjectId objectId) {
            return objectId.toHexString();
        }
        if (value instanceof Date date) {
            return date.toInstant().toString();
        }
        return value;
    }

    private Map<String, Object> toJsonSafeMap(Document document) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : document.entrySet()) {
            result.put(entry.getKey(), toJsonSafe(entry.getValue()));
        }
        return result;
    }

    @PostConstruct
    public void start() {
        Thread worker = new Thread(this::runWatchLoop, "certificates-change-stream");
        worker.setDaemon(true);
        worker.start();
    }

    private void runWatchLoop() {
        refreshAllCertificates();
        while (!Thread.currentThread().isInterrupted()) {
            ChangeStreamIterable<Document> stream = grades.watch()
                    .fullDocument(FullDocument.UPDATE_LOOKUP);
            try (MongoChangeStreamCursor<ChangeStreamDocument<Document>> cursor = stream.cursor()) {
                while (cursor.hasNext()) {
                    ChangeStreamDocument<Document> change = cursor.next();
                    String operation = change.getOperationType() == null
                            ? null : change.getOperationType().getValue();
                    if (!HANDLED_OPERATIONS.contains(operation)) continue;
                    Document fullDocument = change.getFullDocument();
                    if (fullDocument == null) continue;
                    ObjectId studentId = fullDocument.getObjectId("studentId");
                    if (studentId == null) continue;
                    try {
                        refreshCertificate(studentId);
                        LOGGER.info("Certificado actualizado para el estudiante {}", studentId.toHexString());
                    } catch (Exception e) {
                        LOGGER.error("No se pudo actualizar el certificado del estudiante {}",
                                studentId.toHexString(), e);
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Change Stream interrumpido: {}", e.getMessage());
                try {
                    Thread.sleep(RECONNECT_DELAY_MS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void refreshAllCertificates() {
        try {
            grades.aggregate(mergePipeline(null)).toCollection();
            LOGGER.info("Vista materializada certificados_notas reconstruida");
        } catch (Exception e) {
            LOGGER.error("No se pudo reconstruir la vista materializada", e);
        }
    }

    private void refreshCertificate(ObjectId studentId) {
        grades.aggregate(mergePipeline(studentId)).toCollection();
    }

    private List<Document> mergePipeline(ObjectId studentId) {
        List<Document> pipeline = new ArrayList<>();
        if (studentId != null) {
            pipeline.add(new Document("$match", new Document("studentId", studentId)));
        }
        pipeline.add(new Document("$lookup",
                new Document("from", "subjects")
                        .append("localField", "subjectId")
                        .append("foreignField", "_id")
                        .append("as", "subject")));
        pipeline.add(new Document("$unwind", "$subject"));
        pipeline.add(new Document("$lookup",
                new Document("from", "semesters")
                        .append("localField", "semesterId")
                        .append("foreignField", "_id")
                        .append("as", "semester")));
        pipeline.add(new Document("$unwind",
                new Document("path", "$semester").append("preserveNullAndEmptyArrays", true)));
        pipeline.add(new Document("$project",
                new Document("_id", new Document("$toString", "$studentId"))
                        .append("subjectCode", "$subject.code")
                        .append("subjectName", "$subject.name")
                        .append("semesterYear", new Document("$ifNull", List.of("$semester.year", 0)))
                        .append("semesterPeriod", new Document("$ifNull", List.of("$semester.period", "")))
                        .append("grade", "$value")
                        .append("recordedAt", "$recordedAt")));
        pipeline.add(new Document("$sort",
                new Document("semesterYear", 1)
                        .append("semesterPeriod", 1)
                        .append("subjectCode", 1)));
        pipeline.add(new Document("$group",
                new Document("_id", "$_id")
                        .append("entries", new Document("$push",
                                new Document("subjectCode", "$subjectCode")
                                        .append("subjectName", "$subjectName")
                                        .append("semesterYear", "$semesterYear")
                                        .append("semesterPeriod", "$semesterPeriod")
                                        .append("grade", "$grade")
                                        .append("recordedAt", "$recordedAt")))
                        .append("totalRamos", new Document("$sum", 1))
                        .append("sumGrades", new Document("$sum", "$grade"))));
        pipeline.add(new Document("$project",
                new Document("entries", 1)
                        .append("totalRamos", 1)
                        .append("promedioGeneral", new Document("$round",
                                List.of(new Document("$divide", List.of("$sumGrades", "$totalRamos")), 1)))
                        .append("updatedAt", new Date())));
        pipeline.add(new Document("$merge",
                new Document("into", "certificados_notas")
                        .append("on", "_id")
                        .append("whenMatched", "replace")
                        .append("whenNotMatched", "insert")));
        return pipeline;
    }
}
