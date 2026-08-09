package usach.cl.demo.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Repository;
import usach.cl.demo.model.CareerEntity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

@Repository
public class MongoCareerRepository {

    private final MongoCollection<Document> careers;

    public MongoCareerRepository(MongoDatabase mongoDatabase) {
        this.careers = mongoDatabase.getCollection("careers");
    }

    public List<CareerEntity> findAll() {
        List<CareerEntity> result = new ArrayList<>();
        for (Document doc : careers.find()) {
            result.add(mapToCareer(doc));
        }
        return result;
    }

    public CareerEntity findById(String id) {
        if (!ObjectId.isValid(id)) return null;
        Document doc = careers.find(eq("_id", new ObjectId(id))).first();
        return doc == null ? null : mapToCareer(doc);
    }

    public CareerEntity findByCode(String code) {
        Document doc = careers.find(eq("code", code)).first();
        return doc == null ? null : mapToCareer(doc);
    }

    public CareerEntity save(CareerEntity career) {
        if (career.getCode() == null || career.getCode().isBlank()) {
            throw new IllegalArgumentException("El código de la carrera es obligatorio");
        }
        if (career.getName() == null || career.getName().isBlank()) {
            throw new IllegalArgumentException("El nombre de la carrera es obligatorio");
        }
        if (career.getId() != null && ObjectId.isValid(career.getId())) {
            careers.updateOne(eq("_id", new ObjectId(career.getId())),
                    combine(set("code", career.getCode()), set("name", career.getName()), set("updatedAt", new Date())));
            return findById(career.getId());
        }
        Document doc = new Document("_id", new ObjectId())
                .append("code", career.getCode())
                .append("name", career.getName())
                .append("createdAt", new Date());
        careers.insertOne(doc);
        return findById(doc.getObjectId("_id").toHexString());
    }

    public void deleteById(String id) {
        if (ObjectId.isValid(id)) {
            careers.deleteOne(eq("_id", new ObjectId(id)));
        }
    }

    private static CareerEntity mapToCareer(Document doc) {
        CareerEntity career = new CareerEntity();
        career.setId(doc.getObjectId("_id").toHexString());
        career.setCode(doc.getString("code"));
        career.setName(doc.getString("name"));
        return career;
    }
}
