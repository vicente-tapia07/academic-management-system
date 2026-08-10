package usach.cl.demo.service.mongo;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import usach.cl.demo.dto.mongo.StudentDirectoryItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.mongodb.client.model.Sorts.ascending;

/**
 * Directorio de estudiantes de MongoDB (Integrante 4 · Frontend 2).
 *
 * Existe porque la API del Laboratorio 3 no ofrece forma de enumerar
 * estudiantes: GET /api/mongo/students/{id} exige conocer el ObjectId de
 * antemano, lo que hace imposible construir un selector en el frontend.
 *
 * Vive en su propia clase, sin tocar EnrollmentTransactionService (Backend 1)
 * ni ReportAggregationService (Backend 2). Es el mismo criterio que usó el
 * Integrante 2 en el Laboratorio 2 al crear RoomAccessibilityController para
 * no modificar el RoomController del Integrante 1.
 *
 * Usa el driver oficial de MongoDB, sin Spring Data ni ODM, igual que el resto
 * de la capa Mongo del proyecto.
 */
@Service
public class StudentDirectoryService {

    private final MongoCollection<Document> students;
    private final MongoCollection<Document> certificates;

    public StudentDirectoryService(MongoDatabase mongoDatabase) {
        this.students = mongoDatabase.getCollection("students");
        this.certificates = mongoDatabase.getCollection("certificados_notas");
    }

    /**
     * Lista los estudiantes ordenados por apellido y nombre, marcando cuáles
     * ya tienen certificado materializado.
     */
    public List<StudentDirectoryItem> listStudents() {
        try {
            // _id de certificados_notas es el studentId en hex (el $project del
            // pipeline de CertificateChangeStreamService lo guarda con $toString).
            Set<String> withCertificate = new HashSet<>(
                    certificates.distinct("_id", String.class).into(new ArrayList<>())
            );

            List<Document> documents = students.find()
                    .sort(ascending("lastName", "firstName"))
                    .into(new ArrayList<>());

            List<StudentDirectoryItem> result = new ArrayList<>(documents.size());
            for (Document document : documents) {
                String id = document.getObjectId("_id").toHexString();
                result.add(new StudentDirectoryItem(
                        id,
                        document.getString("enrollmentNumber"),
                        document.getString("firstName"),
                        document.getString("lastName"),
                        document.getString("careerCode"),
                        document.getString("academicStatus"),
                        withCertificate.contains(id)
                ));
            }
            return result;
        } catch (MongoException exception) {
            throw new EnrollmentTransactionException(
                    EnrollmentTransactionException.Reason.DATABASE_ERROR,
                    "No se pudo obtener el directorio de estudiantes desde MongoDB",
                    exception
            );
        }
    }
}
