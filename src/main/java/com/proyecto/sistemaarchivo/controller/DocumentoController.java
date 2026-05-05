package com.proyecto.sistemaarchivo.controller;

import com.proyecto.sistemaarchivo.JWT.JwtUtils;
import com.proyecto.sistemaarchivo.model.Documento;
import com.proyecto.sistemaarchivo.model.DocumentoExterno;
import com.proyecto.sistemaarchivo.model.DocumentoPreCarga;
import com.proyecto.sistemaarchivo.model.Usuario;
import com.proyecto.sistemaarchivo.repository.ArchivadorRepository;
import com.proyecto.sistemaarchivo.repository.DocumentoPreCargaRepository;
import com.proyecto.sistemaarchivo.repository.DocumentoRepository;
import com.proyecto.sistemaarchivo.repository.DocumentoExternoRepository;
import com.proyecto.sistemaarchivo.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/documentos")
@CrossOrigin(origins = "*")
public class DocumentoController {

    @Autowired private DocumentoRepository repository;
    @Autowired private DocumentoPreCargaRepository preCargaRepo;
    @Autowired private DocumentoExternoRepository externoRepository;
    @Autowired private ArchivadorRepository archivadorRepository;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private UsuarioRepository usuarioRepository;

    private final DateTimeFormatter formatterFlexible = new DateTimeFormatterBuilder()
            .appendPattern("[dd/MM/yyyy][dd/MM/yy][yyyy-MM-dd][d/M/yy][d/M/yyyy][dd-MM-yyyy][dd-MM-yy]")
            .parseDefaulting(ChronoField.ERA, 1)
            .toFormatter();

    @GetMapping("/buscar")
    public ResponseEntity<?> buscarDocumentos(
            @RequestParam(required = false) String criterio,
            @RequestParam(required = false) String fecha,
            @RequestParam(required = false) Integer idTipo,
            @RequestParam(required = false) Integer idDep,
            @RequestParam(required = false) Integer estado,
            HttpServletRequest request) {

        Integer idDepFinal = idDep;
        if (!esPrivilegiado(request)) {
            Integer idDepToken = obtenerDependenciaToken(request);
            if (idDepToken == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No se pudo determinar la dependencia del usuario"));
            }
            if (idDep != null && !idDep.equals(idDepToken)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No puedes consultar documentos de otra dependencia"));
            }
            idDepFinal = idDepToken;
        }

        LocalDate fechaParseada = (fecha != null && !fecha.isEmpty())
                ? LocalDate.parse(fecha)
                : null;

        List<Map<String, Object>> resultados = repository.filtrarDocumentosFull(
                criterio, fechaParseada, idTipo, idDepFinal, estado
        );

        return ResponseEntity.ok(resultados);
    }

    // 1. RUTA
    @GetMapping("/pendientes-revision")
    public ResponseEntity<?> listarCargasPendientes(HttpServletRequest request) {
        if (!esPrivilegiado(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No tienes permiso para revisar cargas"));
        }
        return ResponseEntity.ok(preCargaRepo.listarCargasPendientes());
    }

    @GetMapping("/total-folios")
    public ResponseEntity<?> obtenerTotalFolios(@RequestParam Integer idArchivador, HttpServletRequest request) {
        try {
            if (!puedeAccederArchivador(idArchivador, request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No tienes permiso para consultar este archivador"));
            }
            Integer total = repository.sumarFoliosPorArchivador(idArchivador);
            return ResponseEntity.ok(Map.of("idArchivador", idArchivador, "totalFolios", (total != null ? total : 0)));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/ultimo-nro-orden")
    public ResponseEntity<?> obtenerUltimoNro(@RequestParam Integer idArchivador, HttpServletRequest request) {
        try {
            if (!puedeAccederArchivador(idArchivador, request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No tienes permiso para consultar este archivador"));
            }
            Integer ultimo = repository.obtenerUltimoNroOrden(idArchivador);
            int ultimoSeguro = (ultimo != null ? ultimo : 0);
            return ResponseEntity.ok(Map.of("ultimoNroOrden", ultimoSeguro, "siguienteNroOrden", (ultimoSeguro + 1)));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/carga-masiva")
    @Transactional
    public ResponseEntity<?> cargaMasivaTemporal(
            @RequestParam(required = false) Integer idArchivador,
            @RequestParam(required = false) Integer idUsuario,
            @RequestParam(required = false) String nombreArchivo,
            @RequestParam(required = false) String referencia,
            @RequestParam(required = false) String dependencia,
            @RequestBody List<Map<String, Object>> payload,
            HttpServletRequest request) {
        try {
            if (idArchivador == null || idUsuario == null) {
                return ResponseEntity.badRequest().body("Faltan IDs obligatorios (Archivador o Usuario)");
            }

            if (!puedeAccederArchivador(idArchivador, request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No tienes permiso para cargar en este archivador"));
            }

            if (!esPrivilegiado(request)) {
                Integer idUsuarioToken = obtenerIdUsuarioToken(request);
                if (idUsuarioToken == null || !idUsuarioToken.equals(idUsuario)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "No puedes cargar como otro usuario"));
                }
            }

            // ============================================================
            // 1. REGISTRAR EN EL HISTORIAL (DocumentoExterno)
            // ============================================================
            DocumentoExterno historial = new DocumentoExterno();
            historial.setIdUsuario(idUsuario);
            historial.setNombreArchivo(getString(nombreArchivo, "archivo_sin_nombre"));
            historial.setFechaCarga(java.time.LocalDateTime.now());
            historial.setEstado(true); // Activo/Cargado
            historial.setFormato("EXCEL");
            historial.setRutaArchivo("CARGA_MASIVA");

            externoRepository.save(historial);

            // ============================================================
            // 2. GUARDAR FILAS EN PRE-CARGA
            // ============================================================
            List<DocumentoPreCarga> listaTemp = new ArrayList<>();
            for (Map<String, Object> fila : payload) {
                DocumentoPreCarga tp = new DocumentoPreCarga();
                tp.setIdArchivador(idArchivador);
                tp.setIdUsuario(idUsuario);
                tp.setAsunto(getString(fila.get("asunto"), "Sin Asunto"));
                tp.setCodigoDocumento(getString(fila.get("codigoDocumento"), "S/N"));
                tp.setTipoDocumentoTexto(getString(fila.get("tipoDocumento"), "OTROS"));
                tp.setDependenciaTexto(getString(dependencia, "OFICINA DESCONOCIDA"));
                tp.setFechaTexto(getString(fila.get("fecha"), ""));
                tp.setFolios(convertToInt(fila.get("folios")));
                tp.setObservacionRevision(getString(fila.get("observacion"), ""));

                String refFinal = getString(referencia, null);
                if (refFinal == null || refFinal.equals("S/R")) {
                    refFinal = getString(fila.get("referencia"), getString(fila.get("remision"), "S/R"));
                }
                tp.setReferencia(refFinal);

                tp.setNombreArchivo(getString(nombreArchivo, "archivo_sin_nombre"));
                tp.setEstadoRevision(0);
                listaTemp.add(tp);
            }
            preCargaRepo.saveAll(listaTemp);

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Carga registrada en historial y enviada a revisión",
                    "totalFilas", listaTemp.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error procesando carga: " + e.getMessage());
        }
    }

    @PostMapping("/aprobar-archivo-completo/{nombreArchivo}")
    @Transactional
    public ResponseEntity<?> aprobarTodoElArchivo(@PathVariable String nombreArchivo, HttpServletRequest request) {
        try {
            if (!esPrivilegiado(request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No tienes permiso para aprobar cargas"));
            }

            List<DocumentoPreCarga> pendientes = preCargaRepo.findByNombreArchivo(nombreArchivo);
            if (pendientes.isEmpty()) return ResponseEntity.notFound().build();

            Integer idArc = pendientes.get(0).getIdArchivador();
            Integer ultimoNro = repository.obtenerUltimoNroOrden(idArc);
            int orden = (ultimoNro != null ? ultimoNro : 0) + 1;

            List<Documento> oficiales = new ArrayList<>();
            for (DocumentoPreCarga t : pendientes) {
                Documento d = new Documento();
                d.setIdArchivador(t.getIdArchivador());
                d.setIdUsuario(t.getIdUsuario());
                d.setAsunto(t.getAsunto());
                d.setNumeroDocumentoOCodigoDocumento(t.getCodigoDocumento());
                d.setNumero_Folio(t.getFolios() != null ? t.getFolios() : 0);
                d.setReferencia(t.getReferencia());
                d.setEstado(1);
                d.setObservacionRevision(t.getObservacionRevision());
                d.setFechaRegistro(LocalDate.now());
                d.setNroOrden(orden++);

                d.setIdTipoDocumento(repository.buscarIdTipoPorNombre(t.getTipoDocumentoTexto().toUpperCase()) != null ? repository.buscarIdTipoPorNombre(t.getTipoDocumentoTexto().toUpperCase()) : 1);
                d.setIdDependencia(repository.buscarIdDependenciaPorNombre(t.getDependenciaTexto().toUpperCase()) != null ? repository.buscarIdDependenciaPorNombre(t.getDependenciaTexto().toUpperCase()) : 1);

                try {
                    if (t.getFechaTexto() != null && !t.getFechaTexto().trim().isEmpty()) {
                        d.setFechaDocumento(LocalDate.parse(t.getFechaTexto().trim(), formatterFlexible));
                    }
                } catch (Exception e) { d.setFechaDocumento(null); }

                oficiales.add(d);
            }
            repository.saveAll(oficiales);
            preCargaRepo.deleteAll(pendientes);

            return ResponseEntity.ok(Map.of("mensaje", "Archivo '" + nombreArchivo + "' aprobado", "total", oficiales.size()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PostMapping("/procesar-revision")
    @Transactional
    public ResponseEntity<?> procesarRevision(@RequestBody Map<String, Object> requestBody, HttpServletRequest request) {
        try {
            if (!esPrivilegiado(request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No tienes permiso para revisar cargas"));
            }

            List<Integer> aprobadosIds = (List<Integer>) requestBody.get("aprobados");
            List<Map<String, Object>> rechazadosList = (List<Map<String, Object>>) requestBody.get("rechazados");

            if (aprobadosIds != null && !aprobadosIds.isEmpty()) {
                List<DocumentoPreCarga> listaAprobados = preCargaRepo.findAllById(aprobadosIds);
                if (!listaAprobados.isEmpty()) {
                    Integer idArc = listaAprobados.get(0).getIdArchivador();
                    Integer ultimoNro = repository.obtenerUltimoNroOrden(idArc);
                    int orden = (ultimoNro != null ? ultimoNro : 0) + 1;

                    List<Documento> oficiales = new ArrayList<>();

                    // Formateador más robusto para detectar varios estilos de fecha del Excel
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("[dd/MM/yyyy][yyyy-MM-dd][d/M/yyyy][dd-MM-yyyy]");

                    for (DocumentoPreCarga t : listaAprobados) {
                        Documento d = new Documento();
                        d.setIdArchivador(t.getIdArchivador());
                        d.setIdUsuario(t.getIdUsuario());
                        d.setAsunto(t.getAsunto());
                        d.setNumeroDocumentoOCodigoDocumento(t.getCodigoDocumento());
                        d.setNumero_Folio(t.getFolios() != null ? t.getFolios() : 0);
                        d.setReferencia(t.getReferencia());
                        d.setObservacionRevision(t.getObservacionRevision());
                        d.setEstado(1);

                        // fechaRegistro es cuando se aprueba (HOY).
                        d.setFechaRegistro(LocalDate.now());
                        d.setNroOrden(orden++);

                        Integer idT = repository.buscarIdTipoPorNombre(t.getTipoDocumentoTexto().toUpperCase());
                        d.setIdTipoDocumento(idT != null ? idT : 1);
                        Integer idD = repository.buscarIdDependenciaPorNombre(t.getDependenciaTexto().toUpperCase());
                        d.setIdDependencia(idD != null ? idD : 1);

                        // ==========================================
                        // LÓGICA DE FECHA
                        // ==========================================
                        try {
                            String fechaRaw = t.getFechaTexto();
                            if (fechaRaw != null && !fechaRaw.trim().isEmpty()) {
                                String fechaLimpia = fechaRaw.trim();

                                // 1. Si termina en algo raro como /247, /248, etc, lo normalizamos a /24
                                if (fechaLimpia.matches(".*\\/\\d{3}$")) {
                                    // Si tiene 3 dígitos al final después del /, quitamos el último
                                    fechaLimpia = fechaLimpia.substring(0, fechaLimpia.length() - 1);
                                }

                                // 2. Intentamos el parseo con el formatter que acepta yy y yyyy
                                d.setFechaDocumento(LocalDate.parse(fechaLimpia, formatterFlexible));
                            }
                        } catch (Exception e) {
                            // Si todo falla, imprimimos para saber por qué, pero no rompemos el proceso
                            System.err.println("No se pudo parsear: " + t.getFechaTexto() + " -> Error: " + e.getMessage());
                            d.setFechaDocumento(null);
                        }

                        oficiales.add(d);
                    }
                    repository.saveAll(oficiales);
                    preCargaRepo.deleteAll(listaAprobados);
                }
            }

            if (rechazadosList != null) {
                for (Map<String, Object> r : rechazadosList) {
                    Integer id = convertToInt(r.get("id"));
                    String motivo = (String) r.get("motivo");
                    preCargaRepo.findById(id).ifPresent(p -> {
                        p.setEstadoRevision(2);
                        p.setObservacionRechazo(motivo);
                        preCargaRepo.save(p);
                    });
                }
            }
            return ResponseEntity.ok(Map.of("mensaje", "Revisión procesada correctamente"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    // =========================================================================
    // 2. RUTAS CON VARIABLES ESPECÍFICAS
    // =========================================================================

    @GetMapping("/archivador/{idArchivador}")
    public ResponseEntity<?> listarPorArchivador(@PathVariable Integer idArchivador, HttpServletRequest request) {
        if (!puedeAccederArchivador(idArchivador, request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No tienes permiso para ver este archivador"));
        }
        return ResponseEntity.ok(repository.listarDocumentosPorArchivadorFull(idArchivador));
    }

    @GetMapping("/detalle-pendiente/{nombreArchivo}")
    public ResponseEntity<?> verDetallePendiente(@PathVariable String nombreArchivo, HttpServletRequest request) {
        if (!esPrivilegiado(request)) {
            Integer idUsuarioToken = obtenerIdUsuarioToken(request);
            if (idUsuarioToken == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No se pudo identificar al usuario"));
            }
            List<DocumentoPreCarga> detalleUsuario = preCargaRepo.findByNombreArchivoAndIdUsuario(nombreArchivo, idUsuarioToken);
            if (detalleUsuario.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No tienes permiso para ver este detalle"));
            }
            return ResponseEntity.ok(detalleUsuario);
        }
        List<DocumentoPreCarga> detalle = preCargaRepo.findByNombreArchivo(nombreArchivo);
        if (detalle.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "No encontrado"));
        return ResponseEntity.ok(detalle);
    }

    @GetMapping("/mis-observados/{idUsuario}")
    public ResponseEntity<?> listarMisObservados(@PathVariable Integer idUsuario, HttpServletRequest request) {
        if (!esPrivilegiado(request)) {
            Integer idUsuarioToken = obtenerIdUsuarioToken(request);
            if (idUsuarioToken == null || !idUsuarioToken.equals(idUsuario)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No tienes permiso para ver observados de otro usuario"));
            }
        }
        return ResponseEntity.ok(preCargaRepo.findByIdUsuarioAndEstadoRevision(idUsuario, 2));
    }

    // =========================================================================
    // 3. RUTAS CON ID GENÉRICO
    // =========================================================================

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerUno(@PathVariable Integer id, HttpServletRequest request) {
        return repository.findById(id).map(doc -> {
            if (!puedeAccederDependencia(doc.getIdDependencia(), request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No tienes permiso para ver este documento"));
            }
            return ResponseEntity.ok(doc);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> crearManual(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        try {
            Integer idArchivador = convertToInt(payload.get("idArchivador"));
            if (!puedeAccederArchivador(idArchivador, request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No tienes permiso para registrar en este archivador"));
            }

            if (!esPrivilegiado(request)) {
                Integer idDepToken = obtenerDependenciaToken(request);
                Integer idDepPayload = convertToInt(payload.get("idDependencia"));
                if (idDepPayload > 0 && !idDepPayload.equals(idDepToken)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "No puedes registrar documentos para otra dependencia"));
                }
                payload.put("idDependencia", idDepToken);
            }

            Documento doc = new Documento();
            mapearDocumento(doc, payload);
            Integer idArc = convertToInt(payload.get("idArchivador"));
            Integer ultimo = repository.obtenerUltimoNroOrden(idArc);
            doc.setNroOrden((ultimo != null ? ultimo : 0) + 1);
            doc.setFechaRegistro(LocalDate.now());
            doc.setEstado(1);
            return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(doc));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> editar(@PathVariable Integer id, @RequestBody Map<String, Object> payload, HttpServletRequest request) {
        return repository.findById(id).map(doc -> {
            if (!puedeAccederDependencia(doc.getIdDependencia(), request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No tienes permiso para editar este documento"));
            }

            if (!esPrivilegiado(request)) {
                Integer idDepToken = obtenerDependenciaToken(request);
                Integer idDepPayload = convertToInt(payload.get("idDependencia"));
                if (idDepPayload > 0 && !idDepPayload.equals(idDepToken)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "No puedes mover el documento a otra dependencia"));
                }
                payload.put("idDependencia", idDepToken);
            }

            mapearDocumento(doc, payload);
            repository.save(doc);
            return ResponseEntity.ok(Map.of("mensaje", "Actualizado con éxito"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> eliminar(@PathVariable Integer id, HttpServletRequest request) {
        return repository.findById(id).map(doc -> {
            if (!puedeAccederDependencia(doc.getIdDependencia(), request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No tienes permiso para eliminar este documento"));
            }

            try {
                repository.delete(doc);
                return ResponseEntity.ok(Map.of("mensaje", "Eliminado físicamente"));
            } catch (Exception e) {
                doc.setEstado(0);
                repository.save(doc);
                return ResponseEntity.ok(Map.of("mensaje", "Desactivado (Borrado lógico)"));
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/corregir-y-reenviar/{id}")
    @Transactional
    public ResponseEntity<?> corregirFila(@PathVariable Integer id, @RequestBody Map<String, Object> payload, HttpServletRequest request) {
        return preCargaRepo.findById(id).map(p -> {
            if (!esPrivilegiado(request)) {
                Integer idUsuarioToken = obtenerIdUsuarioToken(request);
                if (idUsuarioToken == null || !idUsuarioToken.equals(p.getIdUsuario())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "No tienes permiso para corregir este registro"));
                }
            }

            p.setAsunto(getString(payload.get("asunto"), p.getAsunto()));
            p.setCodigoDocumento(getString(payload.get("codigoDocumento"), p.getCodigoDocumento()));
            p.setTipoDocumentoTexto(getString(payload.get("tipoDocumento"), p.getTipoDocumentoTexto()));
            p.setFechaTexto(getString(payload.get("fecha"), p.getFechaTexto()));
            p.setFolios(convertToInt(payload.get("folios")));
            p.setEstadoRevision(0);
            p.setObservacionRechazo(null);
            preCargaRepo.save(p);
            return ResponseEntity.ok(Map.of("mensaje", "Corregido y enviado"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/mis-cargas")
    public ResponseEntity<?> obtenerMisCargas(HttpServletRequest request) {
        try {
            Integer idUsuarioToken = obtenerIdUsuarioToken(request);
            if (idUsuarioToken == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No se pudo obtener la identidad del usuario"));
            }

            List<DocumentoPreCarga> cargas = preCargaRepo.obtenerMisCargas(idUsuarioToken);

            // Mapear a respuesta más clara para el frontend
            List<Map<String, Object>> resultado = cargas.stream().map(c -> {
                DocumentoExterno cargaArchivo = externoRepository
                        .findTopByNombreArchivoOrderByFechaCargaDesc(c.getNombreArchivo())
                        .orElse(null);

                Map<String, Object> m = new java.util.HashMap<>();
                m.put("id", c.getId());
                m.put("nroOrden", c.getId()); // identificador estable mientras no exista un nroOrden real en pre-carga
                m.put("identificadorOrden", c.getId());
                m.put("idArchivador", c.getIdArchivador());
                m.put("idUsuario", c.getIdUsuario());
                m.put("nombreArchivo", c.getNombreArchivo() != null ? c.getNombreArchivo() : "Sin nombre");
                m.put("codigoDocumento", c.getCodigoDocumento() != null ? c.getCodigoDocumento() : "S/N");
                m.put("asunto", c.getAsunto() != null ? c.getAsunto() : "Sin asunto");
                m.put("tipoDocumento", c.getTipoDocumentoTexto() != null ? c.getTipoDocumentoTexto() : "OTROS");
                m.put("dependenciaTexto", c.getDependenciaTexto() != null ? c.getDependenciaTexto() : "");
                m.put("referencia", c.getReferencia() != null ? c.getReferencia() : "");
                m.put("remision", c.getReferencia() != null ? c.getReferencia() : "");
                m.put("folios", c.getFolios() != null ? c.getFolios() : 0);
                m.put("estado", c.getEstadoRevision() == 0 ? "PENDIENTE" : (c.getEstadoRevision() == 2 ? "RECHAZADO" : "DESCONOCIDO"));
                m.put("estadoNumero", c.getEstadoRevision());
                m.put("observacionRevision", c.getObservacionRevision() != null ? c.getObservacionRevision() : "");
                m.put("observacionRechazo", c.getObservacionRechazo() != null ? c.getObservacionRechazo() : "");
                m.put("fechaCarga", c.getFechaTexto() != null ? c.getFechaTexto() : "");
                m.put("fechaCargaArchivo", cargaArchivo != null && cargaArchivo.getFechaCarga() != null ? cargaArchivo.getFechaCarga() : null);
                return m;
            }).toList();

            return ResponseEntity.ok(Map.of(
                    "cargas", resultado,
                    "cantidad", resultado.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al obtener cargas: " + e.getMessage()));
        }
    }

    // --- HELPERS ---
    private void mapearDocumento(Documento doc, Map<String, Object> p) {
        doc.setAsunto(getString(p.get("asunto"), doc.getAsunto()));
        doc.setReferencia(getString(p.get("referencia"), doc.getReferencia()));
        doc.setNumeroDocumentoOCodigoDocumento(getString(p.get("numeroDocumentoOCodigoDocumento"), doc.getNumeroDocumentoOCodigoDocumento()));
        doc.setObservacionRevision(getString(p.get("observacionRevision"), doc.getObservacionRevision()));
        Integer nuevoIdTipo = convertToInt(p.get("idTipoDocumento"));
        Integer nuevoIdDep = convertToInt(p.get("idDependencia"));

        if (nuevoIdTipo > 0) {
            doc.setIdTipoDocumento(nuevoIdTipo);
        }

        if (nuevoIdDep > 0) {
            doc.setIdDependencia(nuevoIdDep);
        }


        doc.setIdArchivador(convertToInt(p.get("idArchivador")) > 0 ? convertToInt(p.get("idArchivador")) : doc.getIdArchivador());
        doc.setNumero_Folio(convertToInt(p.get("Numero_Folio")));

        if (p.get("fechaDocumento") != null && !p.get("fechaDocumento").toString().isEmpty()) {
            doc.setFechaDocumento(LocalDate.parse(p.get("fechaDocumento").toString()));
        }
    }

    private String getString(Object obj, String def) {
        return (obj != null && !obj.toString().isEmpty()) ? obj.toString().trim() : def;
    }

    private Integer convertToInt(Object obj) {
        if (obj == null || obj.toString().isEmpty()) return 0;
        try {
            return Integer.parseInt(obj.toString().replaceAll("[^0-9]", ""));
        } catch (Exception e) { return 0; }
    }

    private boolean esPrivilegiado(HttpServletRequest request) {
        String rol = obtenerRolToken(request);
        return "ADMINISTRADOR".equalsIgnoreCase(rol) || "USUARIOA".equalsIgnoreCase(rol);
    }

    private boolean puedeAccederDependencia(Integer idDependencia, HttpServletRequest request) {
        if (esPrivilegiado(request)) return true;
        Integer idDepToken = obtenerDependenciaToken(request);
        return idDepToken != null && idDepToken.equals(idDependencia);
    }

    private boolean puedeAccederArchivador(Integer idArchivador, HttpServletRequest request) {
        if (idArchivador == null || idArchivador <= 0) return false;
        if (esPrivilegiado(request)) return true;

        Integer idDepToken = obtenerDependenciaToken(request);
        if (idDepToken == null) return false;

        return archivadorRepository.findById(idArchivador)
                .map(a -> idDepToken.equals(a.getIdDependencia()))
                .orElse(false);
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

