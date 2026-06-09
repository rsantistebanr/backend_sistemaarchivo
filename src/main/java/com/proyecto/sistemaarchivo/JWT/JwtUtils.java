package com.proyecto.sistemaarchivo.JWT;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtils {

    // Clave secreta para HS256
    private final String jwtSecret = "clave_super_secreta_para_el_sistema_archivo_2026_Final";
    private final long jwtExpirationMs = 28800000; // 8 horas

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    // --- GENERAR TOKEN ---
    public String generarToken(String username, String rolNombre, Integer idDep, Integer idSuc) {
        return Jwts.builder()
                .subject(username)
                .claim("rol", rolNombre)
                .claim("idDependencia", idDep)
                .claim("idSucursal", idSuc)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey()) // Firma automática con el algoritmo adecuado para la clave
                .compact();
    }

    public <T> T extraerClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claimsResolver.apply(claims);
    }

    public String obtenerUsuarioDelToken(String token) {
        return extraerClaim(token, Claims::getSubject);
    }

    public String obtenerRolDelToken(String token) {
        return extraerClaim(token, claims -> claims.get("rol", String.class));
    }

    public Integer obtenerDependenciaDelToken(String token) {
        return extraerClaim(token, claims -> claims.get("idDependencia", Integer.class));
    }

    public Integer obtenerSucursalDelToken(String token) {
        return extraerClaim(token, claims -> claims.get("idSucursal", Integer.class));
    }

    // --- VALIDACIÓN ---
    public boolean validarToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            System.err.println("Error en la validación del Token JWT: " + e.getMessage());
        }
        return false;
    }
}
