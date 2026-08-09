package usach.cl.demo.dto.mongo;

public record SemesterSummary(
        String id,
        int year,
        String period,
        String status
) {
}
