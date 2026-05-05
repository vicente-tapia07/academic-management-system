package usach.cl.demo.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import usach.cl.demo.model.UserEntity;

import java.security.Key;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtUtils {

    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256); // En producción: variable de entorno
    private final long expirationMs = 86400000; // 24 horas

    public String generateToken(Authentication authentication) {
        UserEntity userPrincipal = (UserEntity) authentication.getPrincipal();
        return Jwts.builder()
                .setSubject(userPrincipal.getUsername()) // email
                .claim("id", userPrincipal.getId())
                .claim("roles", userPrincipal.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority).collect(Collectors.toList()))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }

    public String getEmailFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}