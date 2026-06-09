package com.proyecto.sistemaarchivo.security;

import com.proyecto.sistemaarchivo.JWT.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
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
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            System.out.println("403 EN: " + request.getRequestURI());
                            System.out.println("MOTIVO: " + accessDeniedException.getMessage());
                            response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        })
                )
                // Configuración de permisos
                .authorizeHttpRequests(auth -> auth

                        // Rutas públicas
                        .requestMatchers(HttpMethod.POST, "/api/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/registrar").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/publica").permitAll()
                        .requestMatchers("/api/archivadores", "/api/archivadores/**").permitAll()

                        //RUTAS GET
                        /*.requestMatchers(HttpMethod.GET, "/api/dependencias", "/api/dependencias/*")
                        .hasAnyRole("ADMINISTRADOR", "USUARIOA", "USUARIO")
                        .requestMatchers(HttpMethod.GET, "/api/sucursales", "/api/sucursales/*")
                        .hasAnyRole("ADMINISTRADOR", "USUARIOA", "USUARIO")
                        .requestMatchers(HttpMethod.GET, "/api/tipoarchivadores", "/api/tipoarchivadores/*")
                        .hasAnyRole("ADMINISTRADOR", "USUARIOA", "USUARIO")
                        .requestMatchers(HttpMethod.GET, "/api/tipodocumento" , "/api/tipodocumento/*")
                        .hasAnyRole("ADMINISTRADOR", "USUARIOA", "USUARIO")
                        .requestMatchers(HttpMethod.GET, "/api/usuarios" , "/api/usuarios/*")
                        .hasAnyRole("ADMINISTRADOR", "USUARIOA", "USUARIO")*/

                        .requestMatchers(HttpMethod.GET, "/api/dependencias", "/api/dependencias/**")
                        .hasAnyRole("ADMINISTRADOR", "USUARIOA", "USUARIO")

                        .requestMatchers(HttpMethod.GET, "/api/sucursales", "/api/sucursales/**")
                        .hasAnyRole("ADMINISTRADOR", "USUARIOA", "USUARIO")

                        .requestMatchers(HttpMethod.GET, "/api/tipodependencias", "/api/tipodependencias/**")
                        .hasAnyRole("ADMINISTRADOR", "USUARIOA", "USUARIO")

                        .requestMatchers(HttpMethod.GET, "/api/tipoarchivadores", "/api/tipoarchivadores/**")
                        .hasAnyRole("ADMINISTRADOR", "USUARIOA", "USUARIO")

                        .requestMatchers(HttpMethod.GET, "/api/tipodocumento", "/api/tipodocumento/**")
                        .hasAnyRole("ADMINISTRADOR", "USUARIOA", "USUARIO")

                        .requestMatchers(HttpMethod.GET, "/api/usuarios", "/api/usuarios/**")
                        .hasAnyRole("ADMINISTRADOR", "USUARIOA", "USUARIO")

                        .requestMatchers(HttpMethod.GET, "/api/archivadores", "/api/archivadores/**")
                        .hasAnyRole("ADMINISTRADOR", "USUARIOA", "USUARIO")


                        // RUTAS POST, PUT, ETC
                        .requestMatchers("/api/usuarios/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/roles/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/sucursales/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/dependencias/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/tipodependencias/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/estantes/**").hasAnyRole("ADMINISTRADOR", "USUARIOA")
                        .requestMatchers("/api/tipoarchivadores/**").hasAnyRole("ADMINISTRADOR", "USUARIOA")
                        .requestMatchers("/api/tipodocumento/**").hasAnyRole("ADMINISTRADOR", "USUARIOA")

                        // Modulos (Administrador + Archivo Central + Oficina)
                        .requestMatchers("/api/archivadores/**").hasAnyRole("ADMINISTRADOR", "USUARIOA", "USUARIO")
                        .requestMatchers("/api/cajas/**").hasAnyRole("ADMINISTRADOR", "USUARIOA", "USUARIO")
                        .requestMatchers("/api/documentos/**").hasAnyRole("ADMINISTRADOR", "USUARIOA", "USUARIO")
                        .requestMatchers("/api/transferencias/**").hasAnyRole("ADMINISTRADOR", "USUARIOA", "USUARIO")
                        .requestMatchers("/api/detalletransferencia/**").hasAnyRole("ADMINISTRADOR", "USUARIOA", "USUARIO")
                        // Se soportan ambas rutas para compatibilidad
                        .requestMatchers("/api/documento-externo/**", "/api/documentoexterno/**")
                        .hasAnyRole("ADMINISTRADOR", "USUARIOA", "USUARIO")

                        .anyRequest().authenticated()
                )

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );
        
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