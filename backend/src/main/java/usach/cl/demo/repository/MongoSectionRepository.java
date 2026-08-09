package usach.cl.demo.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Repository;
import usach.cl.demo.model.SectionEntity;
import usach.cl.demo.model.SectionRoom;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

@Repository
public class MongoSectionRepository {

    private final MongoCollection<Document> sections;
    private final MongoCollection<Document> semesters;
    private final MongoCollection<Document> enrollments;
    private final MongoCollection<Document> students;

    public MongoSectionRepository(MongoDatabase mongoDatabase) {
        this.sections = mongoDatabase.getCollection("sections");
        this.semesters = mongoDatabase.getCollection("semesters");
        this.enrollments = mongoDatabase.getCollection("enrollments");
        this.students = mongoDatabase.getCollection("students");
    }

    public List<SectionEntity> findAll() {
        List<SectionEntity> result = new ArrayList<>();
        for (Document doc : sections.find()) {
            result.add(mapToSection(doc));
        }
        return result;
    }

    public SectionEntity findById(String id) {
        if (id == null || !ObjectId.isValid(id)) return null;
        Document doc = sections.find(eq("_id", new ObjectId(id))).first();
        return doc == null ? null : mapToSection(doc);
    }

    public List<SectionEntity> findByProfessorId(Long professorId) {
        List<SectionEntity> result = new ArrayList<>();
        for (Document doc : sections.find(eq("professorId", String.valueOf(professorId)))) {
            result.add(mapToSection(doc));
        }
        return result;
    }

    public List<SectionEntity> findByProfessorIdAndActiveSemester(Long professorId) {
        Map<String, String> semesterStatus = new HashMap<>();
        for (Document sem : semesters.find()) {
            semesterStatus.put(sem.getObjectId("_id").toHexString(), sem.getString("status"));
        }
        List<SectionEntity> result = new ArrayList<>();
        for (Document doc : sections.find(eq("professorId", String.valueOf(professorId)))) {
            SectionEntity section = mapToSection(doc);
            if ("IN_PROGRESS".equals(semesterStatus.get(section.getSemesterId()))) {
                result.add(section);
            }
        }
        return result;
    }

    public List<SectionEntity> findByStudentId(Long studentId) {
        Document student = students.find(eq("userId", studentId)).first();
        if (student == null) return new ArrayList<>();
        ObjectId studentObjectId = student.getObjectId("_id");

        List<Document> activeEnrollments = enrollments.find(and(
                eq("studentId", studentObjectId),
                eq("status", "ACTIVE")
        )).into(new ArrayList<>());

        List<SectionEntity> result = new ArrayList<>();
        for (Document enrollment : activeEnrollments) {
            SectionEntity section = findById(enrollment.getObjectId("sectionId").toHexString());
            if (section != null) result.add(section);
        }
        return result;
    }

    public List<SectionRoom> findDistinctRooms() {
        Map<String, SectionRoom> rooms = new LinkedHashMap<>();
        for (Document doc : sections.find()) {
            Document room = doc.get("room", Document.class);
            if (room == null) continue;
            String code = room.getString("code");
            if (code == null || code.isBlank()) continue;
            rooms.putIfAbsent(code, new SectionRoom(code,
                    room.getString("name"), room.getString("building")));
        }
        return new ArrayList<>(rooms.values());
    }

    public SectionEntity save(SectionEntity section) {
        checkScheduleConflict(section, null);
        if (section.getAvailableSeats() <= 0) {
            section.setAvailableSeats(section.getTotalSeats());
        }
        String status = section.getStatus() == null || section.getStatus().isBlank()
                ? "OPEN" : section.getStatus();
        Document doc = new Document("_id", new ObjectId())
                .append("subjectId", new ObjectId(section.getSubjectId()))
                .append("semesterId", new ObjectId(section.getSemesterId()))
                .append("professorId", String.valueOf(section.getProfessorId()))
                .append("professorName", section.getProfessorName())
                .append("totalSeats", section.getTotalSeats())
                .append("availableSeats", section.getAvailableSeats())
                .append("schedule", new Document()
                        .append("dayOfWeek", section.getDayOfWeek())
                        .append("startTime", section.getStartTime().toString())
                        .append("endTime", section.getEndTime().toString()))
                .append("room", toRoomDocument(section.getRoom()))
                .append("status", status)
                .append("createdAt", new Date());
        sections.insertOne(doc);
        return mapToSection(doc);
    }

    public SectionEntity update(String id, SectionEntity section) {
        SectionEntity existing = findById(id);
        if (existing == null) {
            throw new RuntimeException("Section not found with id: " + id);
        }
        checkScheduleConflict(section, id);
        if (section.getAvailableSeats() < 0) {
            section.setAvailableSeats(0);
        }
        sections.updateOne(eq("_id", new ObjectId(id)), new Document("$set", new Document()
                .append("subjectId", new ObjectId(section.getSubjectId()))
                .append("semesterId", new ObjectId(section.getSemesterId()))
                .append("professorId", String.valueOf(section.getProfessorId()))
                .append("professorName", section.getProfessorName())
                .append("totalSeats", section.getTotalSeats())
                .append("availableSeats", section.getAvailableSeats())
                .append("schedule", new Document()
                        .append("dayOfWeek", section.getDayOfWeek())
                        .append("startTime", section.getStartTime().toString())
                        .append("endTime", section.getEndTime().toString()))
                .append("room", toRoomDocument(section.getRoom()))
                .append("status", section.getStatus())
                .append("updatedAt", new Date())));
        return findById(id);
    }

    public int deleteById(String id) {
        if (id == null || !ObjectId.isValid(id)) return 0;
        return (int) sections.deleteOne(eq("_id", new ObjectId(id))).getDeletedCount();
    }

    private void checkScheduleConflict(SectionEntity section, String excludeId) {
        String professorId = String.valueOf(section.getProfessorId());
        List<Document> candidates = sections.find(eq("professorId", professorId)).into(new ArrayList<>());
        for (Document doc : candidates) {
            if (excludeId != null && doc.getObjectId("_id").toHexString().equals(excludeId)) continue;
            Document schedule = doc.get("schedule", Document.class);
            if (schedule == null) continue;
            Integer day = schedule.getInteger("dayOfWeek");
            if (day == null || !day.equals(section.getDayOfWeek())) continue;
            LocalTime existingStart = LocalTime.parse(schedule.getString("startTime"));
            LocalTime existingEnd = LocalTime.parse(schedule.getString("endTime"));
            if (section.getStartTime().isBefore(existingEnd) && existingStart.isBefore(section.getEndTime())) {
                throw new IllegalArgumentException(
                        "El profesor ya tiene una sección en ese horario (día " + section.getDayName() + " " +
                                existingStart + "-" + existingEnd + ")");
            }
        }
    }

    private static Document toRoomDocument(SectionRoom room) {
        if (room == null) return new Document();
        return new Document("code", room.getCode())
                .append("name", room.getName())
                .append("building", room.getBuilding());
    }

    public static SectionEntity mapToSection(Document doc) {
        SectionEntity section = new SectionEntity();
        section.setId(doc.getObjectId("_id").toHexString());
        section.setSubjectId(doc.getObjectId("subjectId").toHexString());
        section.setSemesterId(doc.getObjectId("semesterId").toHexString());
        String professorId = doc.getString("professorId");
        section.setProfessorId(professorId == null ? null : Long.parseLong(professorId));
        section.setProfessorName(doc.getString("professorName"));
        section.setTotalSeats(doc.getInteger("totalSeats", 0));
        section.setAvailableSeats(doc.getInteger("availableSeats", 0));
        section.setStatus(doc.getString("status"));

        Document schedule = doc.get("schedule", Document.class);
        if (schedule != null) {
            section.setDayOfWeek(schedule.getInteger("dayOfWeek"));
            section.setStartTime(LocalTime.parse(schedule.getString("startTime")));
            section.setEndTime(LocalTime.parse(schedule.getString("endTime")));
        }
        Document room = doc.get("room", Document.class);
        if (room != null) {
            section.setRoom(new SectionRoom(
                    room.getString("code"), room.getString("name"), room.getString("building")));
        }
        return section;
    }
}
