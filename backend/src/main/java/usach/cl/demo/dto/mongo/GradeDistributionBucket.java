package usach.cl.demo.dto.mongo;

public record GradeDistributionBucket(
        String label,
        double rangeStart,
        double rangeEndExclusive,
        long count
) {
}
