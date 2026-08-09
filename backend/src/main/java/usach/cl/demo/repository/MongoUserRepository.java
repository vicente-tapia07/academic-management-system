package usach.cl.demo.repository;

import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Repository;
import usach.cl.demo.model.Role;
import usach.cl.demo.model.UserEntity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Sorts.descending;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

@Repository
public class MongoUserRepository {

    private static final int DUPLICATE_KEY_ERROR = 11000;

    private final MongoCollection<Document> users;

    public MongoUserRepository(MongoDatabase mongoDatabase) {
        this.users = mongoDatabase.getCollection("users");
    }

    public UserEntity findByEmail(String email) {
        Document document = users.find(eq("email", email)).first();
        if (document == null) {
            throw new UsernameNotFoundException("Usuario no encontrado con email: " + email);
        }
        return mapToUser(document);
    }

    public UserEntity findById(int id) {
        Document document = users.find(eq("id", id)).first();
        if (document == null) {
            throw new RuntimeException("Usuario no encontrado con id: " + id);
        }
        return mapToUser(document);
    }

    public boolean existsByEmail(String email) {
        return users.find(eq("email", email)).first() != null;
    }

    public List<UserEntity> findAllByRole(Role role) {
        List<Document> documents = users.find(eq("rol", role.name())).into(new ArrayList<>());
        return documents.stream().map(this::mapToUser).toList();
    }

    public UserEntity save(UserEntity user) {
        int id = nextId();
        Document document = new Document()
                .append("id", id)
                .append("rut", user.getRut())
                .append("email", user.getEmail())
                .append("passwordHash", user.getPassword())
                .append("rol", user.getRole().name())
                .append("createdAt", new Date());
        try {
            users.insertOne(document);
        } catch (MongoWriteException exception) {
            if (exception.getError().getCode() == DUPLICATE_KEY_ERROR) {
                throw new IllegalArgumentException("Email ya esta en uso", exception);
            }
            throw exception;
        }
        return new UserEntity(id, user.getRut(), user.getEmail(), user.getPassword(), user.getRole());
    }

    public void updateUser(int id, String rut, String email) {
        users.updateOne(eq("id", id), combine(set("rut", rut), set("email", email)));
    }

    public void updateCredentials(int id, String email, String rut, String passwordHash) {
        List<Bson> changes = new ArrayList<>();
        if (email != null && !email.isBlank()) changes.add(set("email", email));
        if (rut != null && !rut.isBlank()) changes.add(set("rut", rut));
        if (passwordHash != null && !passwordHash.isBlank()) changes.add(set("passwordHash", passwordHash));
        if (!changes.isEmpty()) {
            users.updateOne(eq("id", id), combine(changes));
        }
    }

    public void deleteById(int id) {
        users.deleteOne(eq("id", id));
    }

    private int nextId() {
        Document last = users.find().sort(descending("id")).limit(1).first();
        return last == null ? 1 : last.get("id", Number.class).intValue() + 1;
    }

    private UserEntity mapToUser(Document document) {
        Number id = document.get("id", Number.class);
        return new UserEntity(
                id.intValue(),
                document.getString("rut"),
                document.getString("email"),
                document.getString("passwordHash"),
                Role.valueOf(document.getString("rol"))
        );
    }
}
