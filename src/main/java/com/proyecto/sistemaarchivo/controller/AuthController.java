package com.proyecto.sistemaarchivo.controller;

import com.proyecto.sistemaarchivo.dto.LoginDTO;
import com.proyecto.sistemaarchivo.dto.RegistroDTO;
import com.proyecto.sistemaarchivo.model.Usuario;
import com.proyecto.sistemaarchivo.repository.UsuarioRepository;
import com.proyecto.sistemaarchivo.JWT.JwtUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    private static final String PASSWORD_PATTERN =
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!¡¿?*._-])(?=\\S+$).{8,}$";

    // --- LOGIN ---
    @PostMapping("/login")
    @Transactional
    public ResponseEntity<?> login(@RequestBody LoginDTO loginDTO) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsuario(loginDTO.getUsuario());

        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("mensaje", "Usuario no encontrado"));
        }

        Usuario usuario = usuarioOpt.get();

        // Verificación de bloqueo
        if (usuario.getBloqueado() != null && usuario.getBloqueado() == 1) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("mensaje", "Tu cuenta está bloqueada. Contacta a soporte técnico."));
        }

        // Validar contraseña
        if (!passwordEncoder.matches(loginDTO.getPassword(), usuario.getPassword())) {
            int intentos = (usuario.getIntentosFallidos() == null ? 0 : usuario.getIntentosFallidos()) + 1;
            usuario.setIntentosFallidos(intentos);

            if (intentos >= 2) {
                usuario.setBloqueado(1);
                usuarioRepository.save(usuario);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("mensaje", "Cuenta bloqueada por superar el límite de intentos."));
            }

            usuarioRepository.save(usuario);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("mensaje", "Contraseña incorrecta. Intento " + intentos + " de 2."));
        }

        // Reseteo de intentos y generar Token con los nuevos parámetros
        usuario.setIntentosFallidos(0);
        usuarioRepository.save(usuario);

        String rolNombre = mapearRol(usuario.getIdRol());

        // Llamada corregida a JwtUtils con los 4 parámetros
        String token = jwtUtils.generarToken(
                usuario.getUsuario(),
                rolNombre,
                usuario.getIdDependencia(),
                usuario.getIdSucursal()
        );

        boolean requiereCambioPassword = esRolConCambioObligatorio(usuario.getIdRol())
                && usuario.getCambioContrasena() != null
                && usuario.getCambioContrasena() == 1;

        return ResponseEntity.ok(Map.of(
                "token", token,
                "usuario", usuario.getUsuario(),
                "rol", rolNombre,
                "idUsuario", usuario.getId(),
                "idDependencia", usuario.getIdDependencia() != null ? usuario.getIdDependencia() : 0,
                "idSucursal", usuario.getIdSucursal() != null ? usuario.getIdSucursal() : 0,
                "requiereCambioPassword", requiereCambioPassword
        ));
    }

    // --- REGISTRAR ---
    @PostMapping("/registrar")
    @Transactional
    public ResponseEntity<?> registrar(@RequestBody RegistroDTO registroDTO) {
        try {
            if (!validarPassword(registroDTO.getPassword())) {
                return ResponseEntity.badRequest().body(Map.of("error", "La contraseña es débil."));
            }

            if (usuarioRepository.findByUsuario(registroDTO.getUsuario()).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El nombre de usuario ya existe"));
            }

            Usuario usuario = new Usuario();
            usuario.setNombre(registroDTO.getNombre());
            usuario.setUsuario(registroDTO.getUsuario());
            usuario.setEmail(registroDTO.getEmail());
            usuario.setTelefono(registroDTO.getTelefono());
            usuario.setPassword(passwordEncoder.encode(registroDTO.getPassword()));

            // Lógica de Rol y limpieza de Admin
            Integer rolAEstablecer = (registroDTO.getIdRol() != null) ? registroDTO.getIdRol() : 3;
            usuario.setIdRol(rolAEstablecer);

            if (rolAEstablecer == 1) {
                usuario.setIdDependencia(null);
                usuario.setIdSucursal(null);
            } else {
                // Para usuarios normales, asignamos valores por defecto si no vienen
                usuario.setIdDependencia(1);
                usuario.setIdSucursal(1);
            }

            usuario.setBloqueado(0);
            usuario.setEstado(1);
            usuario.setCambioContrasena(esRolConCambioObligatorio(rolAEstablecer) ? 1 : 0);
            usuarioRepository.save(usuario);

            String rolNombre = mapearRol(rolAEstablecer);

            // Generar token incluyendo los nulos si es Admin
            String token = jwtUtils.generarToken(
                    usuario.getUsuario(),
                    rolNombre,
                    usuario.getIdDependencia(),
                    usuario.getIdSucursal()
            );

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Usuario registrado con éxito",
                    "token", token,
                    "rol", rolNombre
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno: " + e.getMessage()));
        }
    }

    @PutMapping("/cambiar-password-inicial")
    @Transactional
    public ResponseEntity<?> cambiarPasswordInicial(@RequestBody Map<String, String> body, Authentication authentication) {
        try {
            if (authentication == null || authentication.getName() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No autenticado"));
            }

            Optional<Usuario> usuarioOpt = usuarioRepository.findByUsuario(authentication.getName());
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Usuario no encontrado"));
            }

            Usuario usuario = usuarioOpt.get();
            if (!esRolConCambioObligatorio(usuario.getIdRol())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Este cambio obligatorio no aplica para tu rol"));
            }

            String passwordActual = body.get("passwordActual");
            String passwordNueva = body.get("passwordNueva");

            if (passwordActual == null || passwordActual.trim().isEmpty()
                    || passwordNueva == null || passwordNueva.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "passwordActual y passwordNueva son obligatorios"));
            }

            if (!passwordEncoder.matches(passwordActual, usuario.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "La password actual es incorrecta"));
            }

            if (!validarPassword(passwordNueva)) {
                return ResponseEntity.badRequest().body(Map.of("error", "La nueva password no cumple con la política"));
            }

            if (passwordEncoder.matches(passwordNueva, usuario.getPassword())) {
                return ResponseEntity.badRequest().body(Map.of("error", "La nueva password no puede ser igual a la anterior"));
            }

            usuario.setPassword(passwordEncoder.encode(passwordNueva));
            usuario.setCambioContrasena(0);
            usuario.setIntentosFallidos(0);
            usuarioRepository.save(usuario);

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Password actualizada correctamente",
                    "requiereCambioPassword", false
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno: " + e.getMessage()));
        }
    }

    private boolean validarPassword(String password) {
        if (password == null) return false;
        return Pattern.compile(PASSWORD_PATTERN).matcher(password).matches();
    }

    private boolean esRolConCambioObligatorio(Integer idRol) {
        return idRol != null && (idRol == 2 || idRol == 3);
    }

    private String mapearRol(Integer idRol) {
        if (idRol == null) return "USUARIO";
        return switch (idRol) {
            case 1 -> "ADMINISTRADOR";
            case 2 -> "USUARIOA";
            case 3 -> "USUARIO";
            default -> "USUARIO";
        };
    }
}