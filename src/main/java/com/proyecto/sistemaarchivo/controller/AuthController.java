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


    
        // Patrón Regex: Mínimo 8 caracteres, una mayúscula, una minúscula, un número y un símbolo.
        private static final String PASSWORD_PATTERN =
                "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!¡¿?*._-])(?=\\S+$).{8,}$";



        @PostMapping("/login")
        public ResponseEntity<?> login(@RequestBody LoginDTO loginDTO) {
            // 1. Buscamos al usuario de forma segura (sin lanzar Exception)
            System.out.println("Ingreso al controler");
            Optional<Usuario> usuarioOpt = usuarioRepository.findByUsuario(loginDTO.getUsuario());
            // Si el usuario no existe, respondemos 404 y salimos.
            // No se puede bloquear a alguien que no existe en la BD.
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("mensaje", "Usuario no encontrado"));
            }

            Usuario usuario = usuarioOpt.get();

            // 2. Si el usuario existe, verificamos primero si YA está bloqueado
            if (usuario.getBloqueado() != null && usuario.getBloqueado() == 1) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("mensaje", "Tu cuenta está bloqueada por seguridad. Contacta a soporte técnico."));
            }

            // 3. Validar contraseña y gestionar INTENTOS
            if (!passwordEncoder.matches(loginDTO.getPassword(), usuario.getPassword())) {

                // Calculamos los intentos (si es null, empezamos en 0)
                int intentos = (usuario.getIntentosFallidos() == null ? 0 : usuario.getIntentosFallidos()) + 1;
                usuario.setIntentosFallidos(intentos);

                if (intentos == 1) {
                    usuarioRepository.save(usuario);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Map.of("mensaje", "Contraseña incorrecta. Tienes solo UN intento más."));
                } else if (intentos >= 2) {
                    // BLOQUEO TOTAL al llegar al segundo intento fallido
                    usuario.setBloqueado(1);
                    usuarioRepository.save(usuario);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("mensaje", "Has superado el límite de intentos. Tu cuenta ha sido bloqueada."));
                }

                usuarioRepository.save(usuario);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("mensaje", "Contraseña incorrecta"));
            }

            // 4. ÉXITO: Reseteamos intentos y generamos Token
            usuario.setIntentosFallidos(0);
            usuarioRepository.save(usuario);

            String rolNombre = mapearRol(usuario.getIdRol());
            String token = jwtUtils.generarToken(usuario.getUsuario(), rolNombre);

            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "usuario", usuario.getUsuario(),
                    "rol", rolNombre,
                    "idUsuario", usuario.getId()
            ));
        }
    
        @PostMapping("/registrar")
        public ResponseEntity<?> registrar(@RequestBody RegistroDTO registroDTO) {
            try {
                // 1. VALIDACIÓN DE CONTRASEÑA ROBUSTA
                if (!validarPassword(registroDTO.getPassword())) {
                    return ResponseEntity.badRequest().body(Map.of("error",
                            "La contraseña es débil. Debe tener al menos 8 caracteres, " +
                                    "una mayúscula, una minúscula, un número y un símbolo."));
                }
    
                // 2. VERIFICACIÓN DE USUARIO EXISTENTE (Para evitar duplicados)
                if (usuarioRepository.findByUsuario(registroDTO.getUsuario()).isPresent()) {
                    return ResponseEntity.badRequest().body(Map.of("error", "El nombre de usuario ya existe"));
                }
    
                Usuario usuario = new Usuario();
                usuario.setNombre(registroDTO.getNombre());
                usuario.setUsuario(registroDTO.getUsuario());
                usuario.setEmail(registroDTO.getEmail());
                usuario.setTelefono(registroDTO.getTelefono());
    
                // Encriptamos la contraseña validada
                usuario.setPassword(passwordEncoder.encode(registroDTO.getPassword()));
    
                // ASIGNACIÓN DE ROLES
                Integer rolAEstablecer = (registroDTO.getIdRol() != null) ? registroDTO.getIdRol() : 3;
                usuario.setIdRol(rolAEstablecer);
    
                // VALORES POR DEFECTO (Uso de Integer 0 y 1 para evitar errores de tipo)
                usuario.setIdDependencia(1);
                usuario.setIdSucursal(1);
                usuario.setBloqueado(0); // 0 = Falso
                usuario.setEstado(1);    // 1 = Verdadero
    
                usuarioRepository.save(usuario);
    
                String rolNombre = mapearRol(rolAEstablecer);
                String token = jwtUtils.generarToken(usuario.getUsuario(), rolNombre);
    
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
    
        // Método de validación Regex
        private boolean validarPassword(String password) {
            if (password == null) return false;
            return Pattern.compile(PASSWORD_PATTERN).matcher(password).matches();
        }
    
        // Mapeo de Rol
        private String mapearRol(Integer idRol) {
            if (idRol == null) return "USUARIO";
            return switch (idRol) {
                case 1 -> "ADMINISTRADOR";
                case 2 -> "USUARIOA";
                case 3 -> "USUARIO";
                default -> "USUARIO";
            };
        }

        // Dentro de AuthController.java

        /*@PostMapping("/refresh")
        public ResponseEntity<?> refresh(@RequestBody Map<String, String> payload) {
            String tokenViejo = payload.get("token");

            if (tokenViejo == null || tokenViejo.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El token es obligatorio"));
            }

            String nuevoToken = jwtUtils.refrescarToken(tokenViejo);

            if (nuevoToken != null) {
                // Opcional: Podrías buscar al usuario en la BD para ver si fue bloqueado
                // mientras tenía el token viejo, pero para agilidad, esto es suficiente.
                return ResponseEntity.ok(Map.of("token", nuevoToken));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Token inválido o manipulado. Inicie sesión de nuevo."));
            }
        }*/
    }