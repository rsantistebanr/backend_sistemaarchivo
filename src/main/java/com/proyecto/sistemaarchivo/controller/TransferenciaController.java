package com.proyecto.sistemaarchivo.controller;

import com.proyecto.sistemaarchivo.JWT.JwtUtils;
import com.proyecto.sistemaarchivo.dto.TransferenciaDTO;
import com.proyecto.sistemaarchivo.model.*;
import com.proyecto.sistemaarchivo.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transferencias")
@CrossOrigin(origins = "*")
public class TransferenciaController {

    @Autowired private TransferenciaRepository transRepo;
    @Autowired private DetalleTransferenciaRepository detalleRepo;
    @Autowired private HistorialRevisionRepository historialRepo;
    @Autowired private ArchivadorRepository archivadorRepository;
    @Autowired private DependenciaRepository dependenciaRepository;
    @Autowired private EstanteRepository estanteRepository;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private UsuarioRepository usuarioRepository;

    @PostMapping
    @Transactional
    public ResponseEntity<?> crearTransferencia(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        try {
            List<Map<String, Object>> detallesPayload = getDetallesPayload(payload);
            List<Integer> idsArchivadores = (List<Integer>) payload.get("idsArchivadores");
            if (detallesPayload != null && !detallesPayload.isEmpty()) {
                idsArchivadores = detallesPayload.stream()
                        .map(d -> convertToInt(d.get("idArchivador")))
                        .filter(id -> id != null && id > 0)
                        .toList();
            }
            if (idsArchivadores == null || idsArchivadores.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Debe seleccionar archivadores."));
            }

            // 1. Cálculo de Metros
            List<Archivador> archivadoresBD = archivadorRepository.findAllById(idsArchivadores);
            if (archivadoresBD.size() != idsArchivadores.size()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Uno o más archivadores no existen."));
            }
            java.util.Map<Integer, Archivador> archivadorPorId = archivadoresBD.stream()
                    .collect(java.util.stream.Collectors.toMap(Archivador::getId, a -> a));

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

            t.setIdUsuarioRecepcion(convertToInt(payload.get("idUsuarioRecepcion")));

            Transferencia guardada = transRepo.save(t);

            // 3. Guardar Detalles
            if (detallesPayload != null && !detallesPayload.isEmpty()) {
                for (Map<String, Object> detalle : detallesPayload) {
                    Integer idArchivador = convertToInt(detalle.get("idArchivador"));
                    if (idArchivador == null || idArchivador <= 0) {
                        continue;
                    }
                    Archivador arc = archivadorPorId.get(idArchivador);
                    Estante estante = (arc != null && arc.getIdEstante() != null)
                            ? estanteRepository.findById(arc.getIdEstante()).orElse(null)
                            : null;

                    DetalleTransferencia dt = new DetalleTransferencia();
                    dt.setIdTransferencia(guardada.getId());
                    dt.setIdArchivador(idArchivador);
                    dt.setIdDocumentoExterno(getIntFromKeys(detalle, "idDocumentoExterno", "id_documento_externo", "idDocumento"));
                    dt.setSerieDocumental(getStringFromKeys(detalle, "serieDocumental", "serie_documental", "serie"));
                    dt.setFechaDm(getStringFromKeys(detalle, "fechaDm", "fechaDM", "fecha_dm"));
                    dt.setFechaA(getStringFromKeys(detalle, "fechaA", "fecha_a", "fechaAnio", "fecha_ano"));

                    Integer cantidadFolios = getIntFromKeys(detalle, "cantidadFolios", "cantidad_folios", "folios");
                    if (cantidadFolios == null && arc != null) {
                        cantidadFolios = arc.getCantidad_folio();
                    }
                    dt.setCantidadFolios(cantidadFolios);

                    Integer numeroEstante = getIntFromKeys(detalle, "numeroEstante", "numEstante", "numero_estante");
                    if (numeroEstante == null && estante != null) {
                        numeroEstante = estante.getNum_Estante();
                    }
                    dt.setNumeroEstante(numeroEstante);

                    Integer numeroCuerpo = getIntFromKeys(detalle, "numeroCuerpo", "numCuerpo", "numero_cuerpo");
                    if (numeroCuerpo == null && arc != null) {
                        numeroCuerpo = arc.getNumCuerpo();
                    }
                    if (numeroCuerpo == null && estante != null) {
                        numeroCuerpo = estante.getNum_cuerpo();
                    }
                    dt.setNumeroCuerpo(numeroCuerpo);

                    String nivelBalda = getStringFromKeys(detalle, "nivelBalda", "nivel_balda", "valda");
                    if (nivelBalda == null && arc != null) {
                        nivelBalda = arc.getValda();
                    }
                    if (nivelBalda == null && estante != null) {
                        nivelBalda = estante.getValda();
                    }
                    dt.setNivelBalda(nivelBalda);

                    dt.setObservaciones(getStringFromKeys(detalle, "observaciones", "observacion", "observacionRevision"));
                    detalleRepo.save(dt);
                }
            } else {
                for (Archivador arc : archivadoresBD) {
                    Estante estante = (arc.getIdEstante() != null)
                            ? estanteRepository.findById(arc.getIdEstante()).orElse(null)
                            : null;

                    DetalleTransferencia dt = new DetalleTransferencia();
                    dt.setIdTransferencia(guardada.getId());
                    dt.setIdArchivador(arc.getId());
                    dt.setCantidadFolios(arc.getCantidad_folio());
                    dt.setNumeroEstante(estante != null ? estante.getNum_Estante() : null);
                    dt.setNumeroCuerpo(arc.getNumCuerpo() != null ? arc.getNumCuerpo()
                            : (estante != null ? estante.getNum_cuerpo() : null));
                    dt.setNivelBalda(arc.getValda() != null ? arc.getValda()
                            : (estante != null ? estante.getValda() : null));
                    detalleRepo.save(dt);
                }
            }

            for (Archivador arc : archivadoresBD) {
                arc.setTransferido(1);
            }
            archivadorRepository.saveAll(archivadoresBD);

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Transferencia registrada con éxito",
                    "Metros Lineales", sumaML,
                    "idTransferencia", guardada.getId()
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

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

    @GetMapping("/pendientes")
    public ResponseEntity<?> listarPendientes(HttpServletRequest request) {
        if (!esPrivilegiado(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No tienes permiso para ver transferencias pendientes"));
        }

        List<Transferencia> transferencias = transRepo.findAll();
        List<Map<String, Object>> respuesta = new java.util.ArrayList<>();

        for (Transferencia t : transferencias) {
            List<DetalleTransferencia> detalles = detalleRepo.findByIdTransferencia(t.getId());
            List<Map<String, Object>> pendientes = new java.util.ArrayList<>();

            for (DetalleTransferencia d : detalles) {
                List<HistorialRevision> historial = historialRepo.findByIdDetalleEnvio(d.getId());
                boolean esPendiente = historial.isEmpty()
                        || historial.get(0).getEstado() == null
                        || historial.get(0).getEstado() == 3;

                if (esPendiente) {
                    Archivador archivador = archivadorRepository.findById(d.getIdArchivador()).orElse(null);
                    Estante estante = (archivador != null && archivador.getIdEstante() != null)
                            ? estanteRepository.findById(archivador.getIdEstante()).orElse(null)
                            : null;

                    String codigoArchivador = null;
                    if (archivador != null) {
                        if (archivador.getNumero() != null) {
                            codigoArchivador = String.valueOf(archivador.getNumero());
                        } else if (archivador.getAño() != null && !archivador.getAño().isEmpty()) {
                            codigoArchivador = "ARC-" + archivador.getAño();
                        } else {
                            codigoArchivador = "ARC-" + archivador.getId();
                        }
                    }

                    Integer numeroEstante = d.getNumeroEstante() != null ? d.getNumeroEstante()
                            : (estante != null ? estante.getNum_Estante() : null);
                    Integer numeroCuerpo = d.getNumeroCuerpo() != null ? d.getNumeroCuerpo()
                            : (archivador != null && archivador.getNumCuerpo() != null ? archivador.getNumCuerpo()
                            : (estante != null ? estante.getNum_cuerpo() : null));
                    String nivelBalda = d.getNivelBalda() != null ? d.getNivelBalda()
                            : (archivador != null && archivador.getValda() != null ? archivador.getValda()
                            : (estante != null ? estante.getValda() : null));
                    Integer cantidadFolios = d.getCantidadFolios() != null ? d.getCantidadFolios()
                            : (archivador != null ? archivador.getCantidad_folio() : null);

                    Map<String, Object> det = new java.util.HashMap<>();
                    det.put("idDetalleEnvio", d.getId());
                    det.put("idArchivador", d.getIdArchivador());
                    det.put("codigoArchivador", codigoArchivador);
                    det.put("serieDocumental", d.getSerieDocumental());
                    det.put("fechaDm", d.getFechaDm());
                    det.put("fechaA", d.getFechaA());
                    det.put("cantidadFolios", cantidadFolios);
                    det.put("numeroEstante", numeroEstante);
                    det.put("numeroCuerpo", numeroCuerpo);
                    det.put("nivelBalda", nivelBalda);
                    det.put("observaciones", d.getObservaciones());
                    det.put("idDocumentoExterno", d.getIdDocumentoExterno());
                    pendientes.add(det);
            }
            }

            if (!pendientes.isEmpty()) {
                System.out.println("linea 285 tranfereancia controller");
                String usuarioEnvioNombre = usuarioRepository.findById(t.getIdUsuarioEnvio())
                        .map(Usuario::getNombre)
                        .orElse(null);
                String usuarioRecepcionNombre = t.getIdUsuarioRecepcion() != null
                        ? usuarioRepository.findById(t.getIdUsuarioRecepcion()).map(Usuario::getNombre).orElse(null)
                        : null;
                String dependenciaDestinoNombre = t.getIdDependenciaDestino() != null
                        ? dependenciaRepository.findById(t.getIdDependenciaDestino()).map(Dependencia::getNombre).orElse(null)
                        : null;

                Map<String, Object> item = new java.util.HashMap<>();
                item.put("idTransferencia", t.getId());
                item.put("idUsuarioEnvio", t.getIdUsuarioEnvio());
                item.put("usuarioEnvioNombre", usuarioEnvioNombre);
                item.put("idUsuarioRecepcion", t.getIdUsuarioRecepcion());
                item.put("usuarioRecepcionNombre", usuarioRecepcionNombre);
                item.put("idDependenciaDestino", t.getIdDependenciaDestino());
                item.put("dependenciaDestinoNombre", dependenciaDestinoNombre);
                item.put("observacion", t.getObservacion());
                item.put("fechaTransferencia", t.getFechaTransferencia());
                item.put("metrosLineales", t.getMetrosLineales());
                item.put("cantidadArchivadores", t.getCantidadArchivadores());
                item.put("totalPendientes", pendientes.size());
                item.put("detallesPendientes", pendientes);
                respuesta.add(item);
            }
        }

        return ResponseEntity.ok(respuesta);
    }

    @GetMapping
    public ResponseEntity<?> listarTransferencias(
            @RequestParam(required = false) Integer estado,
            Pageable pageable) {

        try {

            Page<TransferenciaDTO> page = transRepo.findByEstado(estado, pageable);

            return ResponseEntity.ok(Map.of(
                    "content", page.getContent(),
                    "totalElements", page.getTotalElements(),
                    "totalPages", page.getTotalPages(),
                    "page", page.getNumber()
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    private Integer convertToInt(Object obj) {
        if (obj == null || obj.toString().trim().isEmpty()) return null;
        try {
            if (obj instanceof Number) return ((Number) obj).intValue();
            return Integer.parseInt(obj.toString().trim());
        } catch (Exception e) { return null; }
    }

    private Integer getIntFromKeys(Map<String, Object> map, String... keys) {
        if (map == null || keys == null) return null;
        for (String key : keys) {
            if (map.containsKey(key)) {
                Integer value = convertToInt(map.get(key));
                if (value != null) return value;
            }
        }
        return null;
    }

    private String getString(Object obj) {
        return (obj != null && !obj.toString().trim().isEmpty()) ? obj.toString().trim() : null;
    }

    private String getStringFromKeys(Map<String, Object> map, String... keys) {
        if (map == null || keys == null) return null;
        for (String key : keys) {
            if (map.containsKey(key)) {
                String value = getString(map.get(key));
                if (value != null) return value;
            }
        }
        return null;
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

    private List<Map<String, Object>> getDetallesPayload(Map<String, Object> payload) {
        if (payload == null) return null;
        Object detallesObj = payload.get("detalles");
        if (detallesObj == null) {
            detallesObj = payload.get("detallesPendientes");
        }
        if (detallesObj == null) {
            detallesObj = payload.get("detalle");
        }
        if (detallesObj instanceof List) {
            return (List<Map<String, Object>>) detallesObj;
        }
        return null;
    }
}