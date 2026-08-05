package usach.cl.demo.service.mongo;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.BucketOptions;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import usach.cl.demo.dto.mongo.GradeDistributionBucket;
import usach.cl.demo.dto.mongo.PassFailRateItem;
import usach.cl.demo.dto.mongo.SubjectSummary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

/**
 * Servicio de analítica académica sobre MongoDB (Backend2).
 * No usa Spring Data MongoDB ni ningún ODM: construye los pipelines con los
 * builders del driver oficial (Aggregates / Accumulators / Filters), igual
 * que EnrollmentTransactionService de Backend1.
 */
@Service
public class ReportAggregationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportAggregationService.class);
    private static final double PASSING_GRADE = 4.0;

    // Límites de la escala de notas chilena (1.0 a 7.0) usados por $bucket.
    private static final List<Double> GRADE_DISTRIBUTION_BOUNDARIES =
            Arrays.asList(1.0, 4.0, 5.0, 6.0, 7.0001);
    private static final List<String> GRADE_DISTRIBUTION_LABELS =
            Arrays.asList("REPROBADO [1.0-4.0)", "SUFICIENTE [4.0-5.0)", "BUENO [5.0-6.0)", "DISTINGUIDO [6.0-7.0]");

    private final MongoCollection<Document> grades;
    private final MongoCollection<Document> subjects;

    public ReportAggregationService(MongoDatabase mongoDatabase) {
        this.grades = mongoDatabase.getCollection("grades");
        this.subjects = mongoDatabase.getCollection("subjects");
    }

    /**
     * Tasa de aprobación/reprobación por asignatura y semestre.
     * Pipeline: $match (opcional) -> $lookup subjects/semesters -> $unwind ->
     * $group (cuenta aprobados/reprobados con $cond) -> $addFields (tasas con $round) -> $sort.
     *
     * subjectId y semesterId son opcionales: si se omiten, el reporte agrupa
     * TODAS las combinaciones asignatura+semestre presentes en grades.
     */
    public List<PassFailRateItem> passFailRate(String subjectId, String semesterId) {
        try {
            List<Bson> pipeline = new ArrayList<>();

            Bson matchFilter = buildOptionalMatch(subjectId, semesterId);
            if (matchFilter != null) {
                pipeline.add(Aggregates.match(matchFilter));
            }

            pipeline.add(Aggregates.lookup("subjects", "subjectId", "_id", "subject"));
            pipeline.add(Aggregates.unwind("$subject"));
            pipeline.add(Aggregates.lookup("semesters", "semesterId", "_id", "semester"));
            pipeline.add(Aggregates.unwind("$semester"));

            Document groupId = new Document("subjectId", "$subjectId")
                    .append("subjectCode", "$subject.code")
                    .append("subjectName", "$subject.name")
                    .append("semesterId", "$semesterId")
                    .append("semesterYear", "$semester.year")
                    .append("semesterPeriod", "$semester.period");

            Document approvedExpr = new Document("$cond", Arrays.asList(
                    new Document("$gte", Arrays.asList("$value", PASSING_GRADE)), 1, 0));
            Document failedExpr = new Document("$cond", Arrays.asList(
                    new Document("$lt", Arrays.asList("$value", PASSING_GRADE)), 1, 0));

            pipeline.add(Aggregates.group(
                    groupId,
                    Accumulators.sum("totalGraded", 1),
                    Accumulators.sum("approved", approvedExpr),
                    Accumulators.sum("failed", failedExpr),
                    Accumulators.avg("averageGrade", "$value")
            ));

            Document approvalRateExpr = new Document("$round", Arrays.asList(
                    new Document("$multiply", Arrays.asList(
                            new Document("$divide", Arrays.asList("$approved", "$totalGraded")), 100)),
                    1));
            Document failureRateExpr = new Document("$round", Arrays.asList(
                    new Document("$multiply", Arrays.asList(
                            new Document("$divide", Arrays.asList("$failed", "$totalGraded")), 100)),
                    1));
            Document avgRoundedExpr = new Document("$round", Arrays.asList("$averageGrade", 1));

            pipeline.add(Aggregates.addFields(
                    new Field<>("approvalRate", approvalRateExpr),
                    new Field<>("failureRate", failureRateExpr),
                    new Field<>("averageGrade", avgRoundedExpr)
            ));

            pipeline.add(Aggregates.sort(Sorts.ascending(
                    "_id.semesterYear", "_id.semesterPeriod", "_id.subjectCode")));

            List<Document> results = grades.aggregate(pipeline).into(new ArrayList<>());
            return results.stream().map(this::toPassFailRateItem).toList();
        } catch (MongoException exception) {
            throw new EnrollmentTransactionException(
                    EnrollmentTransactionException.Reason.DATABASE_ERROR,
                    "No se pudo calcular la tasa de aprobación/reprobación",
                    exception
            );
        }
    }

    /**
     * Distribución de notas en rangos (1-4, 4-5, 5-6, 6-7) usando $bucket.
     * subjectId y semesterId son opcionales; si ambos se omiten, la distribución
     * se calcula sobre todas las notas registradas.
     */
    public List<GradeDistributionBucket> gradeDistribution(String subjectId, String semesterId) {
        try {
            List<Bson> pipeline = new ArrayList<>();

            Bson matchFilter = buildOptionalMatch(subjectId, semesterId);
            if (matchFilter != null) {
                pipeline.add(Aggregates.match(matchFilter));
            }

            pipeline.add(Aggregates.bucket(
                    "$value",
                    GRADE_DISTRIBUTION_BOUNDARIES,
                    new BucketOptions()
                            .defaultBucket("FUERA_DE_RANGO")
                            .output(Accumulators.sum("count", 1))
            ));

            List<Document> results = grades.aggregate(pipeline).into(new ArrayList<>());
            return mapBucketResults(results);
        } catch (MongoException exception) {
            throw new EnrollmentTransactionException(
                    EnrollmentTransactionException.Reason.DATABASE_ERROR,
                    "No se pudo calcular la distribución de notas",
                    exception
            );
        }
    }

    /**
     * Búsqueda de asignaturas por nombre usando el índice de texto {@code text_subject_name}.
     */
    public List<SubjectSummary> searchSubjects(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("El parámetro de búsqueda 'q' es obligatorio");
        }
        try {
            List<Document> results = subjects.find(Filters.text(query)).into(new ArrayList<>());

            // El stemmer en español del índice de texto no siempre dobla tildes
            // (p. ej. "programacion" no siempre encuentra "Programación").
            // Si la búsqueda por texto no encuentra nada, reintentamos con una
            // expresión regular insensible a mayúsculas y tildes.
            if (results.isEmpty()) {
                results = subjects.find(Filters.regex("name", buildAccentInsensitivePattern(query), "i"))
                        .into(new ArrayList<>());
            }

            return results.stream().map(this::toSubjectSummary).toList();
        } catch (MongoException exception) {
            throw new EnrollmentTransactionException(
                    EnrollmentTransactionException.Reason.DATABASE_ERROR,
                    "No se pudo ejecutar la búsqueda de asignaturas",
                    exception
            );
        }
    }

    /**
     * Construye un patrón regex donde cada vocal simple acepta también su
     * variante con tilde (a -> [aá], e -> [eé], etc.), para tolerar búsquedas
     * sin acentuar contra nombres que sí los llevan (o viceversa).
     */
    private String buildAccentInsensitivePattern(String query) {
        StringBuilder pattern = new StringBuilder();
        for (char c : query.toLowerCase().toCharArray()) {
            switch (c) {
                case 'a' -> pattern.append("[aá]");
                case 'e' -> pattern.append("[eé]");
                case 'i' -> pattern.append("[ií]");
                case 'o' -> pattern.append("[oó]");
                case 'u' -> pattern.append("[uúü]");
                case 'n' -> pattern.append("[nñ]");
                default -> {
                    if (Character.isLetterOrDigit(c)) {
                        pattern.append(c);
                    } else {
                        pattern.append("\\").append(c);
                    }
                }
            }
        }
        return pattern.toString();
    }

    private SubjectSummary toSubjectSummary(Document document) {
        return new SubjectSummary(
                document.getObjectId("_id").toHexString(),
                document.getString("code"),
                document.getString("name"),
                document.getInteger("credits", 0),
                document.getString("careerCode"),
                Boolean.TRUE.equals(document.getBoolean("active"))
        );
    }

    private Bson buildOptionalMatch(String subjectId, String semesterId) {
        List<Bson> filters = new ArrayList<>();
        if (subjectId != null && !subjectId.isBlank()) {
            filters.add(eq("subjectId", requiredObjectId(subjectId, "subjectId")));
        }
        if (semesterId != null && !semesterId.isBlank()) {
            filters.add(eq("semesterId", requiredObjectId(semesterId, "semesterId")));
        }
        if (filters.isEmpty()) {
            return null;
        }
        return filters.size() == 1 ? filters.get(0) : and(filters);
    }

    private PassFailRateItem toPassFailRateItem(Document result) {
        Document id = result.get("_id", Document.class);
        long totalGraded = result.getInteger("totalGraded", 0);
        long approved = result.getInteger("approved", 0);
        long failed = result.getInteger("failed", 0);
        double averageGrade = result.get("averageGrade", Number.class).doubleValue();
        double approvalRate = result.get("approvalRate", Number.class).doubleValue();
        double failureRate = result.get("failureRate", Number.class).doubleValue();

        return new PassFailRateItem(
                id.getObjectId("subjectId").toHexString(),
                id.getString("subjectCode"),
                id.getString("subjectName"),
                id.getObjectId("semesterId").toHexString(),
                id.getInteger("semesterYear"),
                id.getString("semesterPeriod"),
                totalGraded,
                approved,
                failed,
                approvalRate,
                failureRate,
                averageGrade
        );
    }

    private List<GradeDistributionBucket> mapBucketResults(List<Document> results) {
        List<GradeDistributionBucket> buckets = new ArrayList<>();
        for (int i = 0; i < GRADE_DISTRIBUTION_BOUNDARIES.size() - 1; i++) {
            double start = GRADE_DISTRIBUTION_BOUNDARIES.get(i);
            double end = GRADE_DISTRIBUTION_BOUNDARIES.get(i + 1);
            long count = findBucketCount(results, start);
            buckets.add(new GradeDistributionBucket(GRADE_DISTRIBUTION_LABELS.get(i), start, end, count));
        }
        return buckets;
    }

    private long findBucketCount(List<Document> results, double boundary) {
        return results.stream()
                .filter(doc -> doc.get("_id", Number.class) != null
                        && doc.get("_id", Number.class).doubleValue() == boundary)
                .findFirst()
                .map(doc -> doc.get("count", Number.class).longValue())
                .orElse(0L);
    }

    private ObjectId requiredObjectId(String value, String fieldName) {
        if (!ObjectId.isValid(value)) {
            throw new IllegalArgumentException(fieldName + " no es un ObjectId válido");
        }
        return new ObjectId(value);
    }
}
