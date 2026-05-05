package com.proyecto.sistemaarchivo.security;

import com.proyecto.sistemaarchivo.JWT.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // Desactivar CSRF porque usamos JWT
                .csrf(csrf -> csrf.disable())

                // Configuración CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Configuración de permisos
                .authorizeHttpRequests(auth -> auth

                        // Rutas públicas
                        .requestMatchers(HttpMethod.POST, "/api/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/registrar").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/publica").permitAll()

                        //RUTAS GET
                        .requestMatchers(HttpMethod.GET, "/dependencias", "/dependencias/*")
                        .hasAnyRole("ADMINISTRADOR", "USUARIOA", "USUARIO")
                        .requestMatchers(HttpMethod.GET, "/sucursales", "/sucursales/*")
                        .hasAnyRole("ADMINISTRADOR", "USUARIOA", "USUARIO")
                        .requestMatchers(HttpMethod.GET, "/tipoarchivadores", "/tipoarchivadores/*")
                        .hasAnyRole("ADMINISTRADOR", "USUARIOA", "USUARIO")
                        .requestMatchers(HttpMethod.GET, "/tipodocumento" , "/tipodocumento/*")
                        .hasAnyRole("ADMINISTRADOR", "USUARIOA", "USUARIO")
                        .requestMatchers(HttpMethod.GET, "/usuarios" , "/usuarios/*")
                        .hasAnyRole("ADMINISTRADOR", "USUARIOA", "USUARIO")

                        // RUTAS POST, PUT, ETC
                        .requestMatchers("/usuarios/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/roles/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/sucursales/**").hasAnyRole("ADMINISTRADOR", "USUARIOA")
                        .requestMatchers("/dependencias/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/tipodependencias/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/estantes/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/tipoarchivadores/**").hasAnyRole("ADMINISTRADOR", "USUARIOA")
                        .requestMatchers("/tipodocumento/**").hasAnyRole("ADMINISTRADOR", "USUARIOA")

                        // Modulos operativos (Administrador + Archivo Central + Oficina)
                        .requestMatchers("/archivadores/**").hasAnyRole("ADMINISTRADOR", "USUARIOA", "USUARIO")
                        .requestMatchers("/cajas/**").hasAnyRole("ADMINISTRADOR", "USUARIOA", "USUARIO")
                        .requestMatchers("/documentos/**").hasAnyRole("ADMINISTRADOR", "USUARIOA", "USUARIO")
                        .requestMatchers("/transferencias/**").hasAnyRole("ADMINISTRADOR", "USUARIOA", "USUARIO")
                        .requestMatchers("/detalletransferencia/**").hasAnyRole("ADMINISTRADOR", "USUARIOA", "USUARIO")
                        // Se soportan ambas rutas para compatibilidad
                        .requestMatchers("/documento-externo/**", "/documentoexterno/**")
                        .hasAnyRole("ADMINISTRADOR", "USUARIOA", "USUARIO")

                        // Cualquier otra petición requiere autenticación
                        .anyRequest().authenticated()
                )

                // No usar sesiones (JWT es stateless)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        // Filtro JWT antes del filtro de autenticación
        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Configuración CORS
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "Accept"
        ));

        configuration.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    // Encriptador de contraseñas
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // AuthenticationManager para login
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

}