package com.proyecto.sistemaarchivo.controller;

import com.proyecto.sistemaarchivo.dto.LoginDTO;
import com.proyecto.sistemaarchivo.dto.RegistroDTO;
import com.proyecto.sistemaarchivo.model.Usuario;
import com.proyecto.sistemaarchivo.repository.UsuarioRepository;
import com.proyecto.sistemaarchivo.JWT.JwtUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    // En AuthController.java

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDTO loginDTO) {
        Usuario usuario = usuarioRepository.findByUsuario(loginDTO.getUsuario())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!passwordEncoder.matches(loginDTO.getPassword(), usuario.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("mensaje", "Contraseña incorrecta"));
        }

        // Obtenemos el nombre del rol según el ID
        String rolNombre = mapearRol(usuario.getIdRol());
        String token = jwtUtils.generarToken(usuario.getUsuario(), rolNombre);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "usuario", usuario.getUsuario(),
                "rol", rolNombre
        ));
    }

    @PostMapping("/registrar")
    public ResponseEntity<?> registrar(@RequestBody RegistroDTO registroDTO) {
        try {
            Usuario usuario = new Usuario();
            usuario.setNombre(registroDTO.getNombre());
            usuario.setUsuario(registroDTO.getUsuario());
            usuario.setEmail(registroDTO.getEmail());
            usuario.setTelefono(registroDTO.getTelefono());
            usuario.setPassword(passwordEncoder.encode(registroDTO.getPassword()));

            // ASIGNACIÓN DE ROLES
            Integer rolAEstablecer = (registroDTO.getIdRol() != null) ? registroDTO.getIdRol() : 3;
            usuario.setIdRol(rolAEstablecer);

            // VALORES OBLIGATORIOS (Asegúrate de que estos métodos existan en tu Entidad)
            usuario.setIdDependencia(1); // Valor por defecto para que no sea null
            usuario.setIdSucursal(1);    // Valor por defecto
            usuario.setBloqueado(false);
            usuario.setEstado(true);
            // usuario.setFechaIngreso(new Date()); // Si tienes este campo, inicialízalo también

            usuarioRepository.save(usuario);

            String rolNombre = mapearRol(rolAEstablecer);
            String token = jwtUtils.generarToken(usuario.getUsuario(), rolNombre);

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Usuario registrado con éxito",
                    "token", token,
                    "rol", rolNombre
            ));
        } catch (Exception e) {
            // Esto te ayudará a ver en Postman qué salió mal exactamente
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al guardar: " + e.getMessage()));
        }
    }

    // Método auxiliar para no repetir código
    private String mapearRol(Integer idRol) {
        return switch (idRol) {
            case 1 -> "ADMINISTRADOR";
            case 2 -> "USUARIOA";
            default -> "USUARIO";
        };
    }
}



