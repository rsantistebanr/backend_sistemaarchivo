package com.proyecto.sistemaarchivo.JWT;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtils {

    // Asegúrate de que esta clave sea lo suficientemente larga para HS256
    private final String jwtSecret = "clave_super_secreta_para_el_sistema_archivo_2026_Final";

    private final long jwtExpirationMs = 28800000; // 8 hora

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    // Generar token con el claim del rol
    public String generarToken(String username, String rolNombre) {
        return Jwts.builder()
                .setSubject(username)
                .claim("rol", rolNombre) // Importante: Verifica que rolNombre no llegue nulo
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Método genérico para extraer información
    public <T> T extraerClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claimsResolver.apply(claims);
    }

    public String obtenerUsuarioDelToken(String token) {
        return extraerClaim(token, Claims::getSubject);
    }

    // Obtener el rol del token (Aseguramos que el nombre del claim coincida)
    public String obtenerRolDelToken(String token) {
        return extraerClaim(token, claims -> claims.get("rol", String.class));
    }

    /*public String refrescarToken(String tokenAnterior) {
        try {
            // Obtenemos los claims ignorando la expiración para poder leer el rol y usuario
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClzrol);
        } catch (ExpiredJwtException e) {
            // Incluso si expiró, podemos sacar los datos para el refresh
            String username = e.getClaims().getSubject();
            String rol = e.getClaims().get("rol", String.class);
            return generarToken(username, rol);
        } catch (Exception e) {
            return null; // Si la firma es falsa o el token es basura
        }
    }*/
    public boolean validarToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            System.err.println("Firma JWT inválida: " + e.getMessage());
        } catch (ExpiredJwtException e) {
            System.err.println("Token JWT expirado: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            System.err.println("Token JWT no soportate: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("JWT claims string está vacío: " + e.getMessage());
        }
        return false;
    }
}