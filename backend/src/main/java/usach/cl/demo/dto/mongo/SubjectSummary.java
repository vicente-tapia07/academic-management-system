package usach.cl.demo.dto.mongo;

public record SubjectSummary(
        String id,
        String code,
        String name,
        int credits,
        String careerCode,
        boolean active
) {
}
