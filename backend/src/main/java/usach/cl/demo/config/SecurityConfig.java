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
                .sessionManagement(sess -> sess
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // PÚBLICAS
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // PROFESOR + ADMIN: rutas específicas PRIMERO
                        .requestMatchers(HttpMethod.POST, "/api/professors/grade").hasAnyRole("ADMIN", "PROFESSOR")
                        .requestMatchers(HttpMethod.GET,  "/api/professors/reports").hasAnyRole("ADMIN", "PROFESSOR")

                        // NOTAS
                        .requestMatchers(HttpMethod.GET,  "/api/grades").hasAnyRole("ADMIN", "PROFESSOR")
                        .requestMatchers(HttpMethod.POST, "/api/grades").hasAnyRole("ADMIN", "PROFESSOR")
                        .requestMatchers(HttpMethod.PUT,  "/api/grades/**").hasAnyRole("ADMIN", "PROFESSOR")

                        // ADMIN: crear, editar, eliminar profesores
                        .requestMatchers(HttpMethod.POST,   "/api/professors").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/professors/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/professors/**").hasRole("ADMIN")

                        // TODOS AUTENTICADOS: ver profesores (ruta raíz Y subrutas)
                        .requestMatchers(HttpMethod.GET, "/api/professors", "/api/professors/**").authenticated()

                        // ADMIN: gestión de estudiantes
                        .requestMatchers("/api/admins/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST,   "/api/students/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/students/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/students/**").hasRole("ADMIN")

                        // STUDENT + PROFESSOR + ADMIN: leer estudiantes e inscripciones
                        .requestMatchers(HttpMethod.GET, "/api/students/**").hasAnyRole("ADMIN", "PROFESSOR", "STUDENT")
                        .requestMatchers(HttpMethod.GET, "/api/enrollments/**").hasAnyRole("ADMIN", "PROFESSOR", "STUDENT")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
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