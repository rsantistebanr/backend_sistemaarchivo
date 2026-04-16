package com.proyecto.sistemaarchivo.JWT;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtils {

    // Clave secreta para HS256
    private final String jwtSecret = "clave_super_secreta_para_el_sistema_archivo_2026_Final";
    private final long jwtExpirationMs = 28800000; // 8 horas

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    // --- GENERAR TOKEN ---
    // Ahora recibe idDependencia e idSucursal para guardarlos en el payload del JWT
    public String generarToken(String username, String rolNombre, Integer idDep, Integer idSuc) {
        return Jwts.builder()
                .setSubject(username)
                .claim("rol", rolNombre)
                .claim("idDependencia", idDep) // Guardamos la dependencia (será null si es Admin)
                .claim("idSucursal", idSuc)    // Guardamos la sucursal (será null si es Admin)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // --- EXTRACCIÓN DE DATOS ---

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

    public String obtenerRolDelToken(String token) {
        return extraerClaim(token, claims -> claims.get("rol", String.class));
    }

    // Extraer ID de Dependencia (puede retornar null para Admins)
    public Integer obtenerDependenciaDelToken(String token) {
        return extraerClaim(token, claims -> claims.get("idDependencia", Integer.class));
    }

    // Extraer ID de Sucursal (puede retornar null para Admins)
    public Integer obtenerSucursalDelToken(String token) {
        return extraerClaim(token, claims -> claims.get("idSucursal", Integer.class));
    }

    // --- VALIDACIÓN ---

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
            System.err.println("Token JWT no soportado: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("JWT claims string está vacío: " + e.getMessage());
        }
        return false;
    }
}