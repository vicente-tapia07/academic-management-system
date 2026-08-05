package usach.cl.demo.dto.mongo;

public record PassFailRateItem(
        String subjectId,
        String subjectCode,
        String subjectName,
        String semesterId,
        int semesterYear,
        String semesterPeriod,
        long totalGraded,
        long approved,
        long failed,
        double approvalRate,
        double failureRate,
        double averageGrade
) {
}
