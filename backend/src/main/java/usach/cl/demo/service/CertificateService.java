package usach.cl.demo.service;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;

@Service
public class CertificateService {

    private final MongoCollection<Document> students;
    private final MongoCollection<Document> certificates;

    public CertificateService(MongoDatabase mongoDatabase) {
        this.students = mongoDatabase.getCollection("students");
        this.certificates = mongoDatabase.getCollection("certificados_notas");
    }

    public Optional<Document> getCertificateByUserId(Long userId) {
        if (userId == null) return Optional.empty();
        Document student = students.find(eq("userId", userId)).first();
        if (student == null) return Optional.empty();
        String studentHex = student.getObjectId("_id").toHexString();
        return Optional.ofNullable(certificates.find(eq("_id", studentHex)).first());
    }
}
