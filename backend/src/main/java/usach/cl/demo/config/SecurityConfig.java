package usach.cl.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth

                // ── PÚBLICAS ──────────────────────────────────────────────
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                // ── RUTAS ESPECÍFICAS PRIMERO (antes que los wildcards) ───

                // Profesor puede registrar notas y ver reportes
                .requestMatchers(HttpMethod.POST, "/api/professors/grade")
                    .hasRole("PROFESSOR")
                .requestMatchers(HttpMethod.GET, "/api/professors/reports")
                    .hasAnyRole("ADMIN", "PROFESSOR")

                // ── PROFESORES: CRUD solo ADMIN ───────────────────────────
                .requestMatchers(HttpMethod.POST,   "/api/professors")
                    .hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/professors/**")
                    .hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/professors/**")
                    .hasRole("ADMIN")

                // ── NOTAS ─────────────────────────────────────────────────
                .requestMatchers(HttpMethod.GET,  "/api/grades")
                    .hasAnyRole("ADMIN", "PROFESSOR")
                .requestMatchers(HttpMethod.POST, "/api/grades")
                    .hasAnyRole("ADMIN", "PROFESSOR")
                .requestMatchers(HttpMethod.PUT,  "/api/grades/**")
                    .hasAnyRole("ADMIN", "PROFESSOR")

                // ── INSCRIPCIONES ─────────────────────────────────────────
                // PATCH solo ADMIN (cambiar estado de inscripción)
                .requestMatchers(HttpMethod.PATCH, "/api/enrollments/**")
                    .hasRole("ADMIN")
                // POST enroll: admin y estudiante
                .requestMatchers(HttpMethod.POST, "/api/enrollments/**")
                    .hasAnyRole("ADMIN", "STUDENT")
                // DELETE: admin y estudiante
                .requestMatchers(HttpMethod.DELETE, "/api/enrollments/**")
                    .hasAnyRole("ADMIN", "STUDENT")

                // ── ESTUDIANTES ───────────────────────────────────────────
                .requestMatchers(HttpMethod.POST,   "/api/students/**")
                    .hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/students/**")
                    .hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH,  "/api/students/**")
                    .hasAnyRole("ADMIN", "STUDENT")
                .requestMatchers(HttpMethod.DELETE, "/api/students/**")
                    .hasRole("ADMIN")

                // ── EDIFICIOS Y SALAS ─────────────────────────────────────
                .requestMatchers(HttpMethod.POST,   "/api/buildings/**", "/api/rooms/**")
                    .hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/buildings/**", "/api/rooms/**")
                    .hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/buildings/**", "/api/rooms/**")
                    .hasRole("ADMIN")

                // ── ACCESIBILIDAD ─────────────────────────────────────────
                .requestMatchers(HttpMethod.POST,   "/api/accessibility-pois/**")
                    .hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/accessibility-pois/**")
                    .hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/accessibility-pois/**")
                    .hasRole("ADMIN")

                // ── ADMIN general ─────────────────────────────────────────
                .requestMatchers("/api/admins/**").hasRole("ADMIN")

                // ── SECCIONES ─────────────────────────────────────────────
                .requestMatchers(HttpMethod.POST,   "/api/sections/**")
                    .hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/sections/**")
                    .hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/sections/**")
                    .hasRole("ADMIN")

                // ── UBICACIÓN (Integrante 2) ──────────────────────────────
                .requestMatchers(HttpMethod.POST, "/api/location/**")
                    .authenticated()

                // ── REPORTES GEOESPACIALES ────────────────────────────────
                .requestMatchers(HttpMethod.GET,  "/api/reports/**")
                    .authenticated()
                .requestMatchers(HttpMethod.POST, "/api/reports/**")
                    .authenticated()

                // ── TODO LO DEMÁS: solo autenticado ──────────────────────
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
