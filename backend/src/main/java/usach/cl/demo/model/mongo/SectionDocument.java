package usach.cl.demo.model.mongo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SectionDocument {

    public enum SectionStatus {
        OPEN,
        CLOSED,
        CANCELLED
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Schedule {
        private Integer dayOfWeek;
        private String startTime;
        private String endTime;

        Document toDocument() {
            return new Document("dayOfWeek", dayOfWeek)
                    .append("startTime", startTime)
                    .append("endTime", endTime);
        }

        static Schedule fromDocument(Document document) {
            if (document == null) return null;
            return new Schedule(
                    document.getInteger("dayOfWeek"),
                    document.getString("startTime"),
                    document.getString("endTime")
            );
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Room {
        private String code;
        private String name;
        private String building;

        Document toDocument() {
            return new Document("code", code)
                    .append("name", name)
                    .append("building", building);
        }

        static Room fromDocument(Document document) {
            if (document == null) return null;
            return new Room(
                    document.getString("code"),
                    document.getString("name"),
                    document.getString("building")
            );
        }
    }

    private String id;
    private String subjectId;
    private String semesterId;
    private String professorId;
    private String professorName;
    private Integer totalSeats;
    private Integer availableSeats;
    private Schedule schedule;
    private Room room;
    private SectionStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public Document toDocument() {
        Document document = MongoDocumentSupport.documentWithOptionalId(id)
                .append("subjectId", MongoDocumentSupport.objectId(subjectId, "subjectId"))
                .append("semesterId", MongoDocumentSupport.objectId(semesterId, "semesterId"))
                .append("professorId", professorId)
                .append("professorName", professorName)
                .append("totalSeats", totalSeats)
                .append("availableSeats", availableSeats)
                .append("schedule", schedule == null ? null : schedule.toDocument())
                .append("room", room == null ? null : room.toDocument())
                .append("status", status == null ? null : status.name())
                .append("createdAt", MongoDocumentSupport.date(createdAt));

        MongoDocumentSupport.appendOptionalDate(document, "updatedAt", updatedAt);
        return document;
    }

    public static SectionDocument fromDocument(Document document) {
        SectionDocument section = new SectionDocument();
        section.setId(MongoDocumentSupport.objectIdHex(document, "_id"));
        section.setSubjectId(MongoDocumentSupport.objectIdHex(document, "subjectId"));
        section.setSemesterId(MongoDocumentSupport.objectIdHex(document, "semesterId"));
        section.setProfessorId(document.getString("professorId"));
        section.setProfessorName(document.getString("professorName"));
        section.setTotalSeats(document.getInteger("totalSeats"));
        section.setAvailableSeats(document.getInteger("availableSeats"));
        section.setSchedule(Schedule.fromDocument(document.get("schedule", Document.class)));
        section.setRoom(Room.fromDocument(document.get("room", Document.class)));

        String status = document.getString("status");
        section.setStatus(status == null ? null : SectionStatus.valueOf(status));
        section.setCreatedAt(MongoDocumentSupport.instant(document, "createdAt"));
        section.setUpdatedAt(MongoDocumentSupport.instant(document, "updatedAt"));
        return section;
    }
}
