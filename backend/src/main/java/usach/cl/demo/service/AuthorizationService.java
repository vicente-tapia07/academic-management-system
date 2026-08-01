package usach.cl.demo.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {

    private final JdbcTemplate jdbcTemplate;

    public AuthorizationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    public void requireStudentAccess(Authentication authentication, Long studentId) {
        if (isAdmin(authentication)) return;
        requireAuthenticated(authentication);
        Integer matches = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM student st
                JOIN usuario u ON u.id = st.usuario_id
                WHERE st.id = ? AND u.email = ?
                """, Integer.class, studentId, authentication.getName());
        if (matches == null || matches == 0) deny();
    }

    public void requireMongoStudentAccess(Authentication authentication, Long userId) {
        if (isAdmin(authentication)) return;
        requireAuthenticated(authentication);
        if (userId == null) deny();

        Integer matches = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM usuario
                WHERE id = ? AND email = ? AND rol = 'STUDENT'
                """, Integer.class, userId, authentication.getName());
        if (matches == null || matches == 0) deny();
    }

    public void requireProfessorAccess(Authentication authentication, Long professorId) {
        if (isAdmin(authentication)) return;
        requireAuthenticated(authentication);
        Integer matches = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM professor p
                JOIN usuario u ON u.id = p.usuario_id
                WHERE p.id = ? AND u.email = ?
                """, Integer.class, professorId, authentication.getName());
        if (matches == null || matches == 0) deny();
    }

    public void requireEnrollmentStudentAccess(Authentication authentication, Long enrollmentId) {
        if (isAdmin(authentication)) return;
        requireAuthenticated(authentication);
        Integer matches = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM enrollment e
                JOIN student st ON st.id = e.student_id
                JOIN usuario u ON u.id = st.usuario_id
                WHERE e.id = ? AND u.email = ?
                """, Integer.class, enrollmentId, authentication.getName());
        if (matches == null || matches == 0) deny();
    }

    public void requireEnrollmentReadAccess(Authentication authentication, Long enrollmentId) {
        if (isAdmin(authentication)) return;
        if (hasRole(authentication, "ROLE_STUDENT")) {
            requireEnrollmentStudentAccess(authentication, enrollmentId);
            return;
        }
        if (hasRole(authentication, "ROLE_PROFESSOR")) {
            requireProfessorOwnsEnrollment(authentication, enrollmentId);
            return;
        }
        deny();
    }

    public void requireProfessorOwnsSection(Authentication authentication, Long sectionId) {
        if (isAdmin(authentication)) return;
        requireAuthenticated(authentication);
        Integer matches = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM section sec
                JOIN professor p ON p.id = sec.professor_id
                JOIN usuario u ON u.id = p.usuario_id
                WHERE sec.id = ? AND u.email = ?
                """, Integer.class, sectionId, authentication.getName());
        if (matches == null || matches == 0) deny();
    }

    public void requireProfessorOwnsEnrollment(Authentication authentication, Long enrollmentId) {
        if (isAdmin(authentication)) return;
        requireAuthenticated(authentication);
        Integer matches = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM enrollment e
                JOIN section sec ON sec.id = e.section_id
                JOIN professor p ON p.id = sec.professor_id
                JOIN usuario u ON u.id = p.usuario_id
                WHERE e.id = ? AND u.email = ?
                """, Integer.class, enrollmentId, authentication.getName());
        if (matches == null || matches == 0) deny();
    }

    public void requireProfessorRut(Authentication authentication, String professorRut) {
        if (isAdmin(authentication)) return;
        requireAuthenticated(authentication);
        Integer matches = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM usuario WHERE email = ? AND rut = ? AND rol = 'PROFESSOR'",
                Integer.class, authentication.getName(), professorRut);
        if (matches == null || matches == 0) deny();
    }

    private void requireAuthenticated(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) deny();
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> role.equals(authority.getAuthority()));
    }

    private void deny() {
        throw new AccessDeniedException("No tienes permiso para acceder a este recurso");
    }
}
