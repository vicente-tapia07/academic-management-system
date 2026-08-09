package usach.cl.demo.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Repository;
import usach.cl.demo.dto.ProfessorDTO;
import usach.cl.demo.model.ProfessorEntity;
import usach.cl.demo.model.Role;
import usach.cl.demo.model.UserEntity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

@Repository
public class MongoProfessorRepository {

    private final MongoCollection<Document> professors;
    private final MongoUserRepository userRepository;

    public MongoProfessorRepository(MongoDatabase mongoDatabase, MongoUserRepository userRepository) {
        this.professors = mongoDatabase.getCollection("professors");
        this.userRepository = userRepository;
    }

    public List<ProfessorEntity> findAll() {
        List<ProfessorEntity> result = new ArrayList<>();
        for (Document doc : professors.find()) {
            result.add(mapToProfessor(doc));
        }
        return result;
    }

    public ProfessorEntity findById(Long id) {
        if (id == null) return null;
        Document doc = professors.find(eq("userId", id)).first();
        return doc == null ? null : mapToProfessor(doc);
    }

    public ProfessorEntity saveWithUsuario(ProfessorDTO dto, String passwordHash) {
        if (dto.email() == null || dto.email().isBlank()) {
            throw new IllegalArgumentException("El email es obligatorio");
        }
        if (userRepository.existsByEmail(dto.email())) {
            throw new IllegalArgumentException("El email ya está en uso");
        }
        String rut = validRut(dto.rut());
        UserEntity user = userRepository.save(
                new UserEntity(-1, rut, dto.email(), passwordHash, Role.PROFESSOR));

        String[] parts = dto.name().trim().split("\\s+", 2);
        String firstName = parts[0];
        String lastName = parts.length > 1 ? parts[1] : "";

        Document doc = new Document("_id", new ObjectId())
                .append("userId", user.getId())
                .append("firstName", firstName)
                .append("lastName", lastName)
                .append("department", dto.department())
                .append("createdAt", new Date());
        professors.insertOne(doc);

        ProfessorEntity entity = new ProfessorEntity();
        entity.setId((long) user.getId());
        entity.setUsuarioId((long) user.getId());
        entity.setFirstName(firstName);
        entity.setLastName(lastName);
        entity.setDepartment(dto.department());
        return entity;
    }

    public void updateProfessor(Long professorId, String department, String firstName, String lastName) {
        professors.updateOne(eq("userId", professorId),
                combine(set("firstName", firstName),
                        set("lastName", lastName),
                        set("department", department),
                        set("updatedAt", new Date())));
    }

    public void updateCredentials(Long professorId, String email, String rut, String passwordHash) {
        userRepository.updateCredentials(professorId.intValue(), email, rut, passwordHash);
    }

    public void deleteByUserId(Long usuarioId) {
        if (usuarioId == null) return;
        professors.deleteOne(eq("userId", usuarioId));
        userRepository.deleteById(usuarioId.intValue());
    }

    /**
     * Genera un RUT con forma válida (ej. 12345678-K) cuando el DTO no lo trae,
     * porque el validador de users exige el patrón ^[0-9]{7,8}-[0-9Kk]{1}$.
     */
    private static String validRut(String rut) {
        if (rut != null && rut.matches("^[0-9]{7,8}-[0-9Kk]{1}$")) {
            return rut;
        }
        int digits = ThreadLocalRandom.current().nextInt(10_000_000, 99_999_999);
        return digits + "-K";
    }

    private static ProfessorEntity mapToProfessor(Document doc) {
        Object rawUserId = doc.get("userId");
        Long userId = rawUserId instanceof Number n ? n.longValue() : null;
        ProfessorEntity entity = new ProfessorEntity();
        entity.setId(userId);
        entity.setUsuarioId(userId);
        entity.setFirstName(doc.getString("firstName"));
        entity.setLastName(doc.getString("lastName"));
        entity.setDepartment(doc.getString("department"));
        return entity;
    }
}
