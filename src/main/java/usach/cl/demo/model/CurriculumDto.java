package usach.cl.demo.model;

import java.util.List;
import jakarta.annotation.Nonnull;

public record CurriculumDto(@Nonnull int studentId,
                            @Nonnull String studentName,
                            @Nonnull String program,
                            @Nonnull List<SubjectStatus> subjects) {

    public record SubjectStatus(@Nonnull String code,
                                @Nonnull String name,
                                @Nonnull String status) {}
}