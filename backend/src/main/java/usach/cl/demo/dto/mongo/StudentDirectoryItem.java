package usach.cl.demo.dto.mongo;

/**
 * Entrada del directorio de estudiantes de MongoDB (Integrante 4 · Frontend 2).
 *
 * Solo expone lo necesario para poblar el selector del certificado de notas.
 * `hasCertificate` indica si el estudiante ya tiene un documento en la
 * colección materializada `certificados_notas`, para que la interfaz pueda
 * distinguir a quienes todavía no registran ninguna calificación.
 */
public record StudentDirectoryItem(
        String id,
        String enrollmentNumber,
        String firstName,
        String lastName,
        String careerCode,
        String academicStatus,
        boolean hasCertificate
) {}
