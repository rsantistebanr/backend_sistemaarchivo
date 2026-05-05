package com.proyecto.sistemaarchivo.controller;

import com.proyecto.sistemaarchivo.JWT.JwtUtils;
import com.proyecto.sistemaarchivo.model.DocumentoExterno;
import com.proyecto.sistemaarchivo.repository.DocumentoExternoRepository;
import com.proyecto.sistemaarchivo.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/documento-externo")
@CrossOrigin(origins = "*")
public class DocumentoExternoController {

    @Autowired
    private DocumentoExternoRepository repository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/historial")
    public ResponseEntity<?> obtenerHistorial(HttpServletRequest request) {
        try {
            List<DocumentoExterno> historial;
            if (esPrivilegiado(request)) {
                historial = repository.findAllByOrderByFechaCargaDesc();
            } else {
                Integer idUsuarioToken = obtenerIdUsuarioToken(request);
                if (idUsuarioToken == null) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "No se pudo identificar al usuario desde el token"));
                }
                historial = repository.findByIdUsuarioOrderByFechaCargaDesc(idUsuarioToken);
            }
            return ResponseEntity.ok(historial);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al obtener historial: " + e.getMessage()));
        }
    }

    /**
     * VERIFICAR DUPLICADOS:
     */
    @GetMapping("/verificar-nombre")
    public ResponseEntity<?> verificarNombre(@RequestParam String nombreArchivo) {
        boolean existe = repository.existsByNombreArchivo(nombreArchivo);
        return ResponseEntity.ok(Map.of("existe", existe));
    }

    /**
     * OBTENER DETALLE:
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Integer id, HttpServletRequest request) {
        if (esPrivilegiado(request)) {
            return repository.findById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }

        Integer idUsuarioToken = obtenerIdUsuarioToken(request);
        if (idUsuarioToken == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "No autenticado"));
        }

        DocumentoExterno doc = repository.findByIdAndIdUsuario(id, idUsuarioToken).orElse(null);
        if (doc == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No tienes permiso para ver este registro o no existe"));
        }
        return ResponseEntity.ok(doc);
    }

    /**
     * ELIMINAR REGISTRO DEL HISTORIAL:
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarHistorial(@PathVariable Integer id, HttpServletRequest request) {
        try {
            if (esPrivilegiado(request)) {
                if (repository.existsById(id)) {
                    repository.deleteById(id);
                    return ResponseEntity.ok(Map.of("mensaje", "Registro eliminado del historial de cargas"));
                }
                return ResponseEntity.notFound().build();
            }

            Integer idUsuarioToken = obtenerIdUsuarioToken(request);
            if (idUsuarioToken == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "No autenticado"));
            }

            return repository.findByIdAndIdUsuario(id, idUsuarioToken).map(doc -> {
                repository.deleteById(doc.getId());
                return ResponseEntity.ok(Map.of("mensaje", "Registro eliminado correctamente"));
            }).orElse(ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No tienes permiso para eliminar este registro")));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // --- MÉTODOS PRIVADOS DE SEGURIDAD ---

    private boolean esPrivilegiado(HttpServletRequest request) {
        String rol = obtenerRolToken(request);
        return "ADMINISTRADOR".equalsIgnoreCase(rol) || "USUARIOA".equalsIgnoreCase(rol);
    }

    private String obtenerRolToken(HttpServletRequest request) {
        String token = extraerToken(request);
        if (token == null) return null;
        try {
            return jwtUtils.obtenerRolDelToken(token);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer obtenerIdUsuarioToken(HttpServletRequest request) {
        String token = extraerToken(request);
        if (token == null) return null;
        try {
            String username = jwtUtils.obtenerUsuarioDelToken(token);
            // El mapeo .map(u -> u.getId()) es correcto asumiendo que tu Entidad Usuario tiene getId()
            return usuarioRepository.findByUsuario(username).map(u -> u.getId()).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private String extraerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return null;
        return header.substring(7);
    }
}
