package usach.cl.demo.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Repository;
import usach.cl.demo.model.SubjectEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;

@Repository
public class MongoSubjectRepository {

    private final MongoCollection<Document> subjects;

    public MongoSubjectRepository(MongoDatabase mongoDatabase) {
        this.subjects = mongoDatabase.getCollection("subjects");
    }

    public List<SubjectEntity> findAll() {
        List<SubjectEntity> result = new ArrayList<>();
        for (Document doc : subjects.find()) {
            result.add(mapToSubject(doc));
        }
        return result;
    }

    public SubjectEntity findById(String id) {
        if (!ObjectId.isValid(id)) return null;
        Document doc = subjects.find(eq("_id", new ObjectId(id))).first();
        return doc == null ? null : mapToSubject(doc);
    }

    public List<SubjectEntity> findByCareerCode(String careerCode) {
        List<SubjectEntity> result = new ArrayList<>();
        for (Document doc : subjects.find(eq("careerCode", careerCode))) {
            result.add(mapToSubject(doc));
        }
        return result;
    }

    public List<SubjectEntity> findByCareerCodeAndActive(String careerCode) {
        List<SubjectEntity> result = new ArrayList<>();
        for (Document doc : subjects.find(new Document("careerCode", careerCode).append("active", true))) {
            result.add(mapToSubject(doc));
        }
        return result;
    }

    public List<SubjectEntity> search(String query) {
        if (query == null || query.isBlank()) return new ArrayList<>();
        List<SubjectEntity> result = new ArrayList<>();
        for (Document doc : subjects.find(new Document("$text", new Document("$search", query)))
                .projection(new Document("score", new Document("$meta", "textScore")))
                .sort(new Document("score", new Document("$meta", "textScore")))) {
            result.add(mapToSubject(doc));
        }
        return result;
    }

    public List<SubjectEntity> findByIds(Collection<String> ids) {
        List<ObjectId> objectIds = ids.stream()
                .filter(ObjectId::isValid)
                .map(ObjectId::new)
                .collect(Collectors.toList());
        if (objectIds.isEmpty()) return new ArrayList<>();
        List<SubjectEntity> result = new ArrayList<>();
        for (Document doc : subjects.find(in("_id", objectIds))) {
            result.add(mapToSubject(doc));
        }
        return result;
    }

    public Map<String, SubjectEntity> findByIdsAsMap(Collection<String> ids) {
        Map<String, SubjectEntity> result = new HashMap<>();
        for (SubjectEntity subject : findByIds(ids)) {
            result.put(subject.getId(), subject);
        }
        return result;
    }

    public SubjectEntity save(SubjectEntity subject) {
        if (subject.getCode() == null || subject.getCode().isBlank()) {
            throw new IllegalArgumentException("El código de la asignatura es obligatorio");
        }
        if (subject.getName() == null || subject.getName().isBlank()) {
            throw new IllegalArgumentException("El nombre de la asignatura es obligatorio");
        }
        List<ObjectId> prerequisiteIds = new ArrayList<>();
        if (subject.getPrerequisiteIds() != null) {
            for (String prereq : subject.getPrerequisiteIds()) {
                if (ObjectId.isValid(prereq)) {
                    prerequisiteIds.add(new ObjectId(prereq));
                }
            }
        }
        if (subject.getId() != null && ObjectId.isValid(subject.getId())) {
            subjects.updateOne(eq("_id", new ObjectId(subject.getId())),
                    new Document("$set", new Document()
                            .append("code", subject.getCode())
                            .append("name", subject.getName())
                            .append("credits", subject.getCredits())
                            .append("careerCode", subject.getCareerCode())
                            .append("prerequisiteIds", prerequisiteIds)
                            .append("active", subject.isActive())
                            .append("updatedAt", new Date())));
            return findById(subject.getId());
        }
        Document doc = new Document("_id", new ObjectId())
                .append("code", subject.getCode())
                .append("name", subject.getName())
                .append("credits", subject.getCredits())
                .append("careerCode", subject.getCareerCode())
                .append("prerequisiteIds", prerequisiteIds)
                .append("active", subject.isActive())
                .append("createdAt", new Date());
        subjects.insertOne(doc);
        return findById(doc.getObjectId("_id").toHexString());
    }

    public void deleteById(String id) {
        if (ObjectId.isValid(id)) {
            subjects.deleteOne(eq("_id", new ObjectId(id)));
        }
    }

    public static SubjectEntity mapToSubject(Document doc) {
        SubjectEntity subject = new SubjectEntity();
        subject.setId(doc.getObjectId("_id").toHexString());
        subject.setCode(doc.getString("code"));
        subject.setName(doc.getString("name"));
        subject.setCredits(doc.getInteger("credits", 0));
        subject.setCareerCode(doc.getString("careerCode"));
        subject.setActive(Boolean.TRUE.equals(doc.getBoolean("active")));
        List<String> prerequisites = new ArrayList<>();
        Object prereqIds = doc.get("prerequisiteIds");
        if (prereqIds instanceof List<?> raw) {
            for (Object item : raw) {
                if (item instanceof ObjectId oid) {
                    prerequisites.add(oid.toHexString());
                }
            }
        }
        subject.setPrerequisiteIds(prerequisites);
        return subject;
    }
}
