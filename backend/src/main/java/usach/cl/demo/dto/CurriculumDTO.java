package usach.cl.demo.dto;

import java.util.List;
import jakarta.annotation.Nonnull;

public record CurriculumDTO(@Nonnull int studentId,
                            @Nonnull String studentName,
                            @Nonnull String program,
                            @Nonnull List<SubjectStatus> subjects) {

    public record SubjectStatus(@Nonnull String code,
                                @Nonnull String name,
                                @Nonnull String status) {}
}