package usach.cl.demo.service.mongo;

import com.mongodb.client.ChangeStreamIterable;
import com.mongodb.client.MongoChangeStreamCursor;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.MergeOptions;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;

/**
 * Reactividad sobre calificaciones (Backend2).
 * Escucha inserts/updates/replaces en la colección {@code grades} mediante
 * Change Streams (requiere Replica Set, ya configurado por Backend1) y
 * recalcula el certificado de notas del estudiante afectado, publicándolo
 * mediante $merge en la colección materializada {@code certificados_notas}.
 * No usa Spring Data ni ODM: driver oficial de MongoDB, mismo patrón que
 * EnrollmentTransactionService y ReportAggregationService.
 */
@Service
public class CertificateChangeStreamService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CertificateChangeStreamService.class);
    private static final String CERTIFICATES_COLLECTION = "certificados_notas";
    private static final long RETRY_DELAY_MS = 2_000L;

    private final MongoCollection<Document> grades;
    private final MongoCollection<Document> certificates;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "certificate-change-stream");
        thread.setDaemon(true);
        return thread;
    });

    private volatile boolean running = true;
    private volatile MongoChangeStreamCursor<ChangeStreamDocument<Document>> activeCursor;

    public CertificateChangeStreamService(MongoDatabase mongoDatabase) {
        this.grades = mongoDatabase.getCollection("grades");
        this.certificates = mongoDatabase.getCollection(CERTIFICATES_COLLECTION);
    }

    @PostConstruct
    public void start() {
        LOGGER.info("Reconstruyendo certificados de notas existentes antes de iniciar el Change Stream...");
        rebuildAllCertificates();
        executor.submit(this::watchLoop);
    }

    @PreDestroy
    public void stop() {
        running = false;
        MongoChangeStreamCursor<ChangeStreamDocument<Document>> cursor = this.activeCursor;
        if (cursor != null) {
            cursor.close();
        }
        executor.shutdownNow();
    }

    /**
     * Certificado de notas materializado de un estudiante, listo para exponer por API.
     * Convierte los tipos BSON (ObjectId, Date) a tipos JSON-friendly.
     */
    public Map<String, Object> getCertificate(String studentId) {
        if (!ObjectId.isValid(studentId)) {
            throw new IllegalArgumentException("studentId no es un ObjectId válido");
        }
        Document document = certificates.find(eq("_id", new ObjectId(studentId))).first();
        if (document == null) {
            return null;
        }
        return toJsonSafeMap(document);
    }

    // ------------------------------------------------------------------
    // Change Stream
    // ------------------------------------------------------------------

    private void watchLoop() {
        List<Bson> watchPipeline = List.of(
                Aggregates.match(in("operationType", List.of("insert", "update", "replace")))
        );

        while (running) {
            try (MongoChangeStreamCursor<ChangeStreamDocument<Document>> cursor = grades
                    .watch(watchPipeline)
                    .fullDocument(FullDocument.UPDATE_LOOKUP)
                    .cursor()) {

                this.activeCursor = cursor;
                LOGGER.info("Change Stream de 'grades' iniciado, escuchando cambios...");

                while (running && cursor.hasNext()) {
                    ChangeStreamDocument<Document> event = cursor.next();
                    handleChange(event);
                }
            } catch (Exception exception) {
                if (!running) {
                    break;
                }
                LOGGER.error("Change Stream de 'grades' interrumpido, reintentando en {} ms", RETRY_DELAY_MS, exception);
                sleepBeforeRetry();
            }
        }
        LOGGER.info("Change Stream de 'grades' detenido.");
    }

    private void handleChange(ChangeStreamDocument<Document> event) {
        Document fullDocument = event.getFullDocument();
        if (fullDocument == null) {
            return;
        }
        ObjectId studentId = fullDocument.getObjectId("studentId");
        if (studentId == null) {
            return;
        }
        try {
            rebuildCertificate(studentId);
            LOGGER.info("Certificado actualizado reactivamente para studentId={}", studentId.toHexString());
        } catch (Exception exception) {
            LOGGER.error("No se pudo actualizar el certificado para studentId={}", studentId, exception);
        }
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    // ------------------------------------------------------------------
    // Reconstrucción del certificado ($lookup + $group + $merge)
    // ------------------------------------------------------------------

    private void rebuildAllCertificates() {
        List<ObjectId> studentIds = grades.distinct("studentId", ObjectId.class).into(new ArrayList<>());
        for (ObjectId studentId : studentIds) {
            try {
                rebuildCertificate(studentId);
            } catch (Exception exception) {
                LOGGER.error("No se pudo reconstruir el certificado inicial para studentId={}", studentId, exception);
            }
        }
        LOGGER.info("Reconstrucción inicial completa: {} certificados procesados", studentIds.size());
    }

    /**
     * Pipeline: $match (una nota) -> $lookup subjects/semesters -> $sort ->
     * $group (arma el certificado completo del estudiante) -> $merge hacia
     * certificados_notas (upsert por _id = studentId).
     */
    private void rebuildCertificate(ObjectId studentId) {
        List<Bson> pipeline = new ArrayList<>();

        pipeline.add(Aggregates.match(eq("studentId", studentId)));
        pipeline.add(Aggregates.lookup("subjects", "subjectId", "_id", "subject"));
        pipeline.add(Aggregates.unwind("$subject"));
        pipeline.add(Aggregates.lookup("semesters", "semesterId", "_id", "semester"));
        pipeline.add(Aggregates.unwind("$semester"));
        pipeline.add(Aggregates.sort(new Document("semester.year", 1)
                .append("semester.period", 1)
                .append("subject.code", 1)));

        Document entryExpr = new Document("subjectCode", "$subject.code")
                .append("subjectName", "$subject.name")
                .append("semesterYear", "$semester.year")
                .append("semesterPeriod", "$semester.period")
                .append("grade", "$value")
                .append("recordedAt", "$recordedAt");

        pipeline.add(Aggregates.group(
                "$studentId",
                Accumulators.push("entries", entryExpr),
                Accumulators.avg("promedioGeneral", "$value"),
                Accumulators.sum("totalRamos", 1)
        ));

        pipeline.add(Aggregates.addFields(new com.mongodb.client.model.Field<>(
                "promedioGeneral", new Document("$round", Arrays.asList("$promedioGeneral", 1)))));
        pipeline.add(Aggregates.addFields(new com.mongodb.client.model.Field<>(
                "updatedAt", "$$NOW")));

        pipeline.add(Aggregates.merge(CERTIFICATES_COLLECTION,
                new MergeOptions()
                        .whenMatched(MergeOptions.WhenMatched.REPLACE)
                        .whenNotMatched(MergeOptions.WhenNotMatched.INSERT)));

        // $merge se ejecuta al materializar el cursor; toCollection() es la
        // forma idiomática del driver para pipelines que terminan en $merge/$out.
        grades.aggregate(pipeline).toCollection();
    }

    // ------------------------------------------------------------------
    // Conversión BSON -> tipos JSON-friendly (para exponer por REST)
    // ------------------------------------------------------------------

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
}
