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
                        // PÚBLICAS
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // ADMIN: Gestión de Profesores (Crea, edita, elimina)
                        .requestMatchers(HttpMethod.POST,   "/api/professors").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/professors/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/professors/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")

                        // ADMIN: Gestión de Inscripciones (PATCH para estados y DELETE para cancelar)
                        .requestMatchers(HttpMethod.POST,   "/api/enrollments/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_STUDENT", "STUDENT")
                        .requestMatchers(HttpMethod.PATCH,  "/api/enrollments/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/enrollments/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_STUDENT", "STUDENT")

                        // PROFESOR + ADMIN: Notas y Reportes
                        .requestMatchers(HttpMethod.POST, "/api/professors/grade").hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_PROFESSOR", "PROFESSOR")
                        .requestMatchers(HttpMethod.GET,  "/api/professors/reports").hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_PROFESSOR", "PROFESSOR")
                        .requestMatchers(HttpMethod.GET,  "/api/grades").hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_PROFESSOR", "PROFESSOR")
                        .requestMatchers(HttpMethod.POST, "/api/grades").hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_PROFESSOR", "PROFESSOR")
                        .requestMatchers(HttpMethod.PUT,  "/api/grades/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_PROFESSOR", "PROFESSOR")

                        // ADMIN: Estudiantes, Edificios, Salas, Accesibilidad
                        .requestMatchers("/api/admins/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.POST,   "/api/students/**", "/api/buildings/**", "/api/rooms/**", "/api/accessibility-pois/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/students/**", "/api/buildings/**", "/api/rooms/**", "/api/accessibility-pois/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/students/**", "/api/buildings/**", "/api/rooms/**", "/api/accessibility-pois/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")

                        // GET GLOBALES (Cualquier usuario logeado puede leer estas listas)
                        .requestMatchers(HttpMethod.GET, "/api/professors/**", "/api/students/**", "/api/enrollments/**", "/api/buildings/**", "/api/rooms/**", "/api/accessibility-pois/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/location/**").authenticated()

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        // Aquí se habilita explícitamente PATCH, vital para cambiar el estado de la inscripción
        config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}