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

                        // Solo ADMINISTRADOR
                        .requestMatchers("/usuarios/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/roles/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/sucursales/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/dependencias/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/tipodependencias/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/estantes/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/archivadores/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/tipoarchivadores/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/documentos/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/tipodocumento/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/transferencias/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/detalletransferencia/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/documentoexterno/**").hasRole("ADMINISTRADOR")

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