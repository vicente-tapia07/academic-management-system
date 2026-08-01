package usach.cl.demo.model.mongo;

import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.Date;

final class MongoDocumentSupport {

    private MongoDocumentSupport() {
    }

    static Document documentWithOptionalId(String id) {
        Document document = new Document();
        if (id != null && !id.isBlank()) {
            document.append("_id", objectId(id, "id"));
        }
        return document;
    }

    static ObjectId objectId(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " es obligatorio");
        }
        if (!ObjectId.isValid(value)) {
            throw new IllegalArgumentException(fieldName + " no es un ObjectId válido");
        }
        return new ObjectId(value);
    }

    static String objectIdHex(Document document, String fieldName) {
        ObjectId value = document.getObjectId(fieldName);
        return value == null ? null : value.toHexString();
    }

    static Date date(Instant value) {
        return value == null ? null : Date.from(value);
    }

    static Instant instant(Document document, String fieldName) {
        Date value = document.getDate(fieldName);
        return value == null ? null : value.toInstant();
    }

    static Long longValue(Document document, String fieldName) {
        Number value = document.get(fieldName, Number.class);
        return value == null ? null : value.longValue();
    }

    static void appendOptionalDate(Document document, String fieldName, Instant value) {
        if (value != null) {
            document.append(fieldName, date(value));
        }
    }
}
