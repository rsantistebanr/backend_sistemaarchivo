package com.proyecto.sistemaarchivo.controller;

import com.proyecto.sistemaarchivo.model.Documento;
import com.proyecto.sistemaarchivo.model.DocumentoExterno;
import com.proyecto.sistemaarchivo.repository.DocumentoRepository;
import com.proyecto.sistemaarchivo.repository.DocumentoExternoRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/documentos")
@CrossOrigin(origins = "*")
public class DocumentoController {

    @Autowired
    private DocumentoRepository repository;

    @Autowired
    private DocumentoExternoRepository externoRepository;

    // --- NUEVO MÉTODO: CARGA MASIVA CON HISTORIAL ---
    @PostMapping("/carga-masiva")
    @Transactional
    public ResponseEntity<?> cargaMasiva(
            @RequestParam Integer idArchivador,
            @RequestParam Integer idUsuario,
            @RequestParam String nombreArchivo,
            // Agregamos estos para que Spring no se confunda al recibirlos
            @RequestParam(required = false) String dependencia,
            @RequestParam(required = false) String remision,
            @RequestParam(required = false) String codigoDependencia,
            @RequestParam(required = false) String anio,
            @RequestBody List<Map<String, Object>> payload) {

        // 1. Validar que los datos críticos no sean nulos
        if (idArchivador == null || idArchivador <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "ID de Archivador no válido"));
        }

        try {
            // 1. Registro de historial (mismo código anterior)
            if (externoRepository.existsByNombreArchivo(nombreArchivo)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "El archivo '" + nombreArchivo + "' ya fue procesado."));
            }

            DocumentoExterno historial = new DocumentoExterno();
            historial.setIdUsuario(idUsuario);
            historial.setNombreArchivo(nombreArchivo);
            historial.setFechaCarga(LocalDateTime.now());
            historial.setEstado(true);
            historial.setFormato("xlsx");
            externoRepository.save(historial);

            // 2. Procesar las filas
            List<Documento> listaNuevos = new ArrayList<>();
            Integer ultimoNro = repository.obtenerUltimoNroOrden(idArchivador);
            int contadorOrden = (ultimoNro != null ? ultimoNro : 0) + 1;

            for (Map<String, Object> fila : payload) {
                Documento doc = new Documento();
                doc.setIdArchivador(idArchivador);
                doc.setIdUsuario(idUsuario);
                doc.setEstado(1);
                doc.setFechaRegistro(LocalDate.now());
                doc.setNroOrden(contadorOrden++);

                // --- TRADUCCIÓN DE TEXTO A ID (La solución) ---

                // 1. Buscar ID del Tipo de Documento por su nombre (ej: "Informe")
                String nombreTipo = getString(fila.get("tipoDocumento"), "OTROS");
                Integer idTipo = repository.buscarIdTipoPorNombre(nombreTipo.toUpperCase());
                doc.setIdTipoDocumento(idTipo != null ? idTipo : 1); // 1 como fallback si no existe

                // 2. Buscar ID de la Dependencia por su nombre (ej: "OFICINA DE...")
                String nombreDep = getString(fila.get("dependencia"), "");
                Integer idDep = repository.buscarIdDependenciaPorNombre(nombreDep.toUpperCase());
                doc.setIdDependencia(idDep != null ? idDep : 1); // 1 como fallback

                // --- RESTO DEL MAPEO ---
                doc.setAsunto(getString(fila.get("asunto"), "Sin Asunto"));
                doc.setNumeroDocumentoOCodigoDocumento(getString(fila.get("codigoDocumento"), null));
                doc.setNumero_Folio(convertToInt(fila.get("folios")));
                doc.setObservacionRevision(getString(fila.get("observacion"), ""));

                // GUARDAR LA REMISIÓN: Usamos el valor capturado de la URL
                doc.setReferencia(remision != null ? remision : "");

                // Manejo de fecha (dd/MM/yy)
                String fStr = getString(fila.get("fecha"), null);
                if (fStr != null) {
                    try {
                        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("[dd/MM/yyyy][dd/MM/yy]");
                        doc.setFechaDocumento(LocalDate.parse(fStr, fmt));
                    } catch (Exception e) {
                        doc.setFechaDocumento(LocalDate.now());
                    }
                }
                listaNuevos.add(doc);
            }

            repository.saveAll(listaNuevos);
            return ResponseEntity.ok(Map.of("mensaje", "Carga masiva completada", "total", listaNuevos.size()));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // --- TUS MÉTODOS ORIGINALES ---

    @GetMapping("/total-folios")
    public ResponseEntity<?> obtenerTotalFolios(@RequestParam Integer idArchivador) {
        try {
            Integer total = repository.sumarFoliosPorArchivador(idArchivador);
            int totalSeguro = (total != null) ? total : 0;
            return ResponseEntity.ok(Map.of("idArchivador", idArchivador, "totalFolios", totalSeguro));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/ultimo-nro-orden")
    public ResponseEntity<?> obtenerUltimoNro(@RequestParam Integer idArchivador) {
        try {
            Integer ultimo = repository.obtenerUltimoNroOrden(idArchivador);
            int ultimoSeguro = (ultimo != null) ? ultimo : 0;
            return ResponseEntity.ok(Map.of("ultimoNroOrden", ultimoSeguro, "siguienteNroOrden", (ultimoSeguro + 1)));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> crear(@RequestBody Map<String, Object> payload) {
        try {
            Documento doc = new Documento();
            Integer idArchivador = convertToInt(payload.get("idArchivador"));
            if (idArchivador == null) return ResponseEntity.badRequest().body(Map.of("error", "Falta idArchivador"));

            mapearDocumento(doc, payload);

            if (doc.getIdUsuario() == null) return ResponseEntity.badRequest().body(Map.of("error", "Falta idUsuario"));

            if (doc.getNumeroDocumentoOCodigoDocumento() != null && repository.existeNumeroDoc(doc.getNumeroDocumentoOCodigoDocumento())) {
                return ResponseEntity.badRequest().body(Map.of("error", "El código ya existe"));
            }

            if (doc.getNroOrden() == null || doc.getNroOrden() == 0) {
                Integer ultimo = repository.obtenerUltimoNroOrden(idArchivador);
                doc.setNroOrden((ultimo != null ? ultimo : 0) + 1);
            }

            doc.setFechaRegistro(LocalDate.now());
            if (doc.getEstado() == null) doc.setEstado(1);

            Documento guardado = repository.save(doc);
            return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> editar(@PathVariable Integer id, @RequestBody Map<String, Object> payload) {
        return repository.findById(id).map(doc -> {
            try {
                mapearDocumento(doc, payload);
                repository.save(doc);
                return ResponseEntity.ok(Map.of("mensaje", "Actualizado con éxito"));
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    // --- ENDPOINT PARA VISUALIZACIÓN ---

    @GetMapping("/archivador/{idArchivador}")
    public ResponseEntity<?> listarPorArchivador(@PathVariable Integer idArchivador) {
        try {
            // Validamos que el ID sea correcto
            if (idArchivador == null || idArchivador <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "ID de archivador no válido"));
            }

            // Obtenemos la lista con nombres de tipos y dependencias
            List<Map<String, Object>> documentos = repository.listarDocumentosPorArchivadorFull(idArchivador);

            if (documentos.isEmpty()) {
                return ResponseEntity.ok(new ArrayList<>()); // Devolvemos lista vacía si no hay nada
            }

            return ResponseEntity.ok(documentos);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Error al recuperar documentos: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        return repository.findById(id).map(doc -> {
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

    private void mapearDocumento(Documento doc, Map<String, Object> p) {
        doc.setAsunto(getString(p.get("asunto"), doc.getAsunto()));
        doc.setReferencia(getString(p.get("referencia"), doc.getReferencia()));
        doc.setRutaArchivo(getString(p.get("rutaArchivo"), doc.getRutaArchivo()));
        doc.setNumeroDocumentoOCodigoDocumento(getString(p.get("numeroDocumentoOCodigoDocumento"), doc.getNumeroDocumentoOCodigoDocumento()));
        doc.setIdTipoDocumento(convertToInt(p.get("idTipoDocumento")));
        doc.setIdArchivador(convertToInt(p.get("idArchivador")));
        doc.setIdDependencia(convertToInt(p.get("idDependencia")));
        doc.setIdUsuario(convertToInt(p.get("idUsuario")));
        doc.setNroOrden(convertToInt(p.get("nroOrden")));
        doc.setNumero_Folio(convertToInt(p.get("Numero_Folio")));
        doc.setEstado(convertToInt(p.getOrDefault("estado", doc.getEstado())));

        if (p.get("fechaDocumento") != null && !p.get("fechaDocumento").toString().isEmpty()) {
            doc.setFechaDocumento(LocalDate.parse(p.get("fechaDocumento").toString()));
        }
    }

    private String getString(Object obj, String defaultValue) {
        return (obj != null) ? obj.toString().trim() : defaultValue;
    }

    private Integer convertToInt(Object obj) {
        if (obj == null || obj.toString().trim().isEmpty()) return null;
        try {
            return Integer.parseInt(obj.toString().trim());
        } catch (NumberFormatException e) { return null; }
    }
}