package com.proyecto.sistemaarchivo.controller;

import com.proyecto.sistemaarchivo.dto.UsuarioDTO;
import com.proyecto.sistemaarchivo.model.Usuario;
import com.proyecto.sistemaarchivo.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // 1. LISTAR USUARIOS
    @GetMapping
    public List<UsuarioDTO> listar() {
        List<Map<String, Object>> usuariosRaw = usuarioRepository.listarUsuariosConNombres();
        return usuariosRaw.stream().map(this::convertirADTO).collect(Collectors.toList());
    }

    // 2. CREAR NUEVO USUARIO
    @PostMapping
    @Transactional
    public ResponseEntity<?> crearUsuario(@RequestBody Usuario nuevoUsuario) {
        try {
            if (usuarioRepository.existsByUsuario(nuevoUsuario.getUsuario())) {
                return ResponseEntity.badRequest().body(Map.of("error", "El nombre de usuario ya está en uso."));
            }

            if (nuevoUsuario.getPassword() == null || !validarPassword(nuevoUsuario.getPassword())) {
                return ResponseEntity.badRequest().body(Map.of("error", "La contraseña no cumple con los requisitos mínimos."));
            }

            Integer rolNuevo = (nuevoUsuario.getIdRol() != null) ? nuevoUsuario.getIdRol() : 3;
            nuevoUsuario.setIdRol(rolNuevo);

            // LÓGICA DE ADMINISTRADOR: Si es Rol 1, limpiar dependencia y sucursal
            if (rolNuevo == 1) {
                nuevoUsuario.setIdDependencia(null);
                nuevoUsuario.setIdSucursal(null);
            }

            nuevoUsuario.setPassword(passwordEncoder.encode(nuevoUsuario.getPassword()));

            if (nuevoUsuario.getEstado() == null) nuevoUsuario.setEstado(1);
            if (nuevoUsuario.getBloqueado() == null) nuevoUsuario.setBloqueado(0);
            if (nuevoUsuario.getIntentosFallidos() == null) nuevoUsuario.setIntentosFallidos(0);
            nuevoUsuario.setCambioContrasena(esRolConCambioObligatorio(rolNuevo) ? 1 : 0);

            usuarioRepository.save(nuevoUsuario);
            return ResponseEntity.ok(Map.of("mensaje", "Usuario creado con éxito"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error en el servidor: " + e.getMessage()));
        }
    }

    // 3. OBTENER POR ID
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Integer id) {
        return usuarioRepository.findById(id).map(usuario -> {
            Map<String, Object> body = new HashMap<>();
            body.put("id", usuario.getId());
            body.put("usuario", usuario.getUsuario());
            body.put("nombre", usuario.getNombre());
            body.put("email", usuario.getEmail());
            body.put("telefono", usuario.getTelefono());
            body.put("idRol", usuario.getIdRol());
            body.put("idSucursal", usuario.getIdSucursal());
            body.put("idDependencia", usuario.getIdDependencia());
            body.put("estado", usuario.getEstado());
            body.put("bloqueado", usuario.getBloqueado());
            return ResponseEntity.ok(body);
        }).orElse(ResponseEntity.notFound().build());
    }

    // 4. EDITAR USUARIO
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> editarUsuario(@PathVariable Integer id, @RequestBody Usuario detalles) {
        return usuarioRepository.findById(id).map(usuario -> {
            usuario.setNombre(detalles.getNombre());
            usuario.setUsuario(detalles.getUsuario());
            usuario.setEmail(detalles.getEmail());
            usuario.setTelefono(detalles.getTelefono());
            usuario.setIdRol(detalles.getIdRol());
            usuario.setEstado(detalles.getEstado());

            // LÓGICA DE ADMINISTRADOR EN EDICIÓN
            if (detalles.getIdRol() != null && detalles.getIdRol() == 1) {
                usuario.setIdDependencia(null);
                usuario.setIdSucursal(null);
                usuario.setCambioContrasena(0);
            } else {
                usuario.setIdDependencia(detalles.getIdDependencia());
                usuario.setIdSucursal(detalles.getIdSucursal());
            }

            if (detalles.getBloqueado() != null) {
                if (detalles.getBloqueado() == 0) usuario.setIntentosFallidos(0);
                usuario.setBloqueado(detalles.getBloqueado());
            }

            if (detalles.getPassword() != null && !detalles.getPassword().trim().isEmpty()) {
                if (!validarPassword(detalles.getPassword())) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Contraseña inválida."));
                }
                usuario.setPassword(passwordEncoder.encode(detalles.getPassword()));
                usuario.setCambioContrasena(esRolConCambioObligatorio(usuario.getIdRol()) ? 1 : 0);
            }

            usuarioRepository.save(usuario);
            return ResponseEntity.ok(Map.of("mensaje", "Usuario actualizado correctamente"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // 5. CAMBIAR BLOQUEO (PATCH) - CORREGIDO
    @PatchMapping("/{id}/bloqueo")
    @Transactional
    public ResponseEntity<?> cambiarBloqueado(@PathVariable Integer id, @RequestBody Map<String, Integer> body) {
        Integer nuevoEstado = body.get("bloqueado");
        return usuarioRepository.findById(id).map(usuario -> {
            usuario.setBloqueado(nuevoEstado);
            if (nuevoEstado == 0) usuario.setIntentosFallidos(0);

            // CORRECCIÓN: Usar el repository para guardar
            usuarioRepository.save(usuario);

            String msg = (nuevoEstado == 0) ? "Usuario desbloqueado" : "Usuario bloqueado";
            return ResponseEntity.ok(Map.of("mensaje", msg));
        }).orElse(ResponseEntity.notFound().build());
    }

    // 6. ELIMINAR
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarUsuario(@PathVariable Integer id) {
        return usuarioRepository.findById(id).map(u -> {
            usuarioRepository.delete(u);
            return ResponseEntity.ok(Map.of("mensaje", "Usuario eliminado"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // 7. BUSCAR
    @GetMapping("/buscar")
    public List<UsuarioDTO> buscar(@RequestParam String criterio) {
        List<Map<String, Object>> usuariosRaw = usuarioRepository.buscarUsuariosPorCriterio(criterio);
        return usuariosRaw.stream().map(this::convertirADTO).collect(Collectors.toList());
    }

    // --- MÉTODOS AUXILIARES ---

    private UsuarioDTO convertirADTO(Map<String, Object> row) {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setId((Integer) row.get("id"));
        dto.setNombre((String) row.get("nombre"));
        dto.setUsuario((String) row.get("usuario"));
        dto.setEmail((String) row.get("email"));
        dto.setTelefono((String) row.get("telefono"));
        dto.setBloqueado((Integer) row.get("bloqueado"));

        Integer estadoNum = (Integer) row.get("estado");
        dto.setEstado(estadoNum != null && estadoNum == 1 ? "Activo" : "Inactivo");

        Integer idRol = (Integer) row.get("IdRol");
        if (idRol != null && idRol == 1) {
            dto.setRol("ADMINISTRADOR");
            dto.setDependencia("N/A");
            dto.setSucursal("N/A");
        } else {
            dto.setRol((String) row.get("nombre_rol"));
            dto.setDependencia(row.get("nombre_dependencia") != null ? (String) row.get("nombre_dependencia") : "Sin Dependencia");
            dto.setSucursal(row.get("nombre_sucursal") != null ? (String) row.get("nombre_sucursal") : "Sin Sucursal");
        }
        return dto;
    }

    private boolean validarPassword(String password) {
        String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!¡¿?*._-])(?=\\S+$).{8,}$";
        return java.util.regex.Pattern.compile(PASSWORD_PATTERN).matcher(password).matches();
    }

    private boolean esRolConCambioObligatorio(Integer idRol) {
        return idRol != null && (idRol == 2 || idRol == 3);
    }
}