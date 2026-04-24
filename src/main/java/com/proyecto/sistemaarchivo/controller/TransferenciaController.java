package com.proyecto.sistemaarchivo.controller;

import com.proyecto.sistemaarchivo.JWT.JwtUtils;
import com.proyecto.sistemaarchivo.model.*;
import com.proyecto.sistemaarchivo.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/transferencias")
@CrossOrigin(origins = "*")
public class TransferenciaController {

    @Autowired private TransferenciaRepository transRepo;
    @Autowired private DetalleTransferenciaRepository detalleRepo;
    @Autowired private HistorialRevisionRepository historialRepo;
    @Autowired private ArchivadorRepository archivadorRepository;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private UsuarioRepository usuarioRepository;

    @PostMapping
    @Transactional
    public ResponseEntity<?> crearTransferencia(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        try {
            List<Integer> idsArchivadores = (List<Integer>) payload.get("idsArchivadores");
            if (idsArchivadores == null || idsArchivadores.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Debe seleccionar archivadores."));
            }

            // 1. Cálculo de Metros
            List<Archivador> archivadoresBD = archivadorRepository.findAllById(idsArchivadores);
            if (archivadoresBD.size() != idsArchivadores.size()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Uno o más archivadores no existen."));
            }

            if (!esPrivilegiado(request)) {
                Integer idDepToken = obtenerDependenciaToken(request);
                if (idDepToken == null) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "No se pudo determinar la dependencia del usuario"));
                }
                boolean fueraDeDependencia = archivadoresBD.stream()
                        .anyMatch(a -> !idDepToken.equals(a.getIdDependencia()));
                if (fueraDeDependencia) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "Solo puedes transferir archivadores de tu dependencia"));
                }
            }

            Double sumaML = archivadoresBD.stream()
                    .mapToDouble(a -> a.getUnidadMedida() != null ? a.getUnidadMedida() : 0.0)
                    .sum();

            // 2. Guardar Transferencia (Cabecera)
            Transferencia t = new Transferencia();
            Integer idUsuarioEnvio = convertToInt(payload.get("idUsuarioEnvio"));
            if (!esPrivilegiado(request)) {
                Integer idUsuarioToken = obtenerIdUsuarioToken(request);
                if (idUsuarioToken == null) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "No se pudo determinar el usuario autenticado"));
                }
                if (idUsuarioEnvio != null && !idUsuarioEnvio.equals(idUsuarioToken)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "No puedes enviar transferencias con otro usuario"));
                }
                idUsuarioEnvio = idUsuarioToken;
            }
            t.setIdUsuarioEnvio(idUsuarioEnvio);
            t.setIdDependenciaDestino(convertToInt(payload.get("idDependenciaDestino")));
            t.setObservacion((String) payload.getOrDefault("observacion", "Envío de archivadores"));
            t.setMetrosLineales(sumaML);
            t.setCantidadArchivadores(archivadoresBD.size());
            t.setFechaTransferencia(LocalDateTime.now());

            // AGREGADO: IdUsuarioRecepcion (Se captura del payload si ya se sabe quién recibe, sino queda null)
            t.setIdUsuarioRecepcion(convertToInt(payload.get("idUsuarioRecepcion")));

            Transferencia guardada = transRepo.save(t);

            // 3. Guardar Detalles
            for (Archivador arc : archivadoresBD) {
                DetalleTransferencia dt = new DetalleTransferencia();
                dt.setIdTransferencia(guardada.getId());
                dt.setIdArchivador(arc.getId());
                // Si vienes de una carga masiva de documentos, aquí podrías asignar el IdDocumentoExterno
                detalleRepo.save(dt);
            }

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Transferencia registrada con éxito",
                    "Metros Lineales", sumaML,
                    "idTransferencia", guardada.getId()
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // Para evitar el error de "MissingServletRequestParameter",
    // asegúrate de que el parámetro sea opcional o venía de otra lógica
    @PutMapping("/revisar/{idDetalleEnvio}")
    @Transactional
    public ResponseEntity<?> revisarArchivador(
            @PathVariable Integer idDetalleEnvio,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            if (!esPrivilegiado(request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No tienes permiso para revisar transferencias"));
            }

            List<HistorialRevision> lista = historialRepo.findByIdDetalleEnvio(idDetalleEnvio);

            HistorialRevision h = lista.isEmpty() ? new HistorialRevision() : lista.get(0);

            h.setIdDetalleEnvio(idDetalleEnvio);
            h.setEstado(convertToInt(body.get("estado")));
            h.setIdUsuarioRevision(convertToInt(body.get("idUsuarioRevision")));
            h.setFecha_Revision(LocalDateTime.now());

            if (body.get("subsanar") != null && (boolean) body.get("subsanar")) {
                h.setFecha_SubSancion(LocalDateTime.now());
            }

            historialRepo.save(h);
            return ResponseEntity.ok(Map.of("mensaje", "Revisión procesada correctamente"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    private Integer convertToInt(Object obj) {
        if (obj == null || obj.toString().trim().isEmpty()) return null;
        try {
            if (obj instanceof Number) return ((Number) obj).intValue();
            return Integer.parseInt(obj.toString().trim());
        } catch (Exception e) { return null; }
    }

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

    private Integer obtenerDependenciaToken(HttpServletRequest request) {
        String token = extraerToken(request);
        if (token == null) return null;
        try {
            return jwtUtils.obtenerDependenciaDelToken(token);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer obtenerIdUsuarioToken(HttpServletRequest request) {
        String token = extraerToken(request);
        if (token == null) return null;
        try {
            String username = jwtUtils.obtenerUsuarioDelToken(token);
            return usuarioRepository.findByUsuario(username).map(Usuario::getId).orElse(null);
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