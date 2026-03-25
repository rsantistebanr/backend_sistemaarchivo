package com.proyecto.sistemaarchivo.controller;

import com.proyecto.sistemaarchivo.model.Documento;
import com.proyecto.sistemaarchivo.repository.DocumentoRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/documentos")
@CrossOrigin(origins = "*")
public class DocumentoController {

    @Autowired
    private DocumentoRepository repository;

    @GetMapping("/total-folios")
    public ResponseEntity<?> obtenerTotalFolios(@RequestParam Integer idArchivador) {
        try {
            Integer total = repository.sumarFoliosPorArchivador(idArchivador);
            // Evitar null si el archivador está vacío
            int totalSeguro = (total != null) ? total : 0;

            return ResponseEntity.ok(Map.of(
                    "idArchivador", idArchivador,
                    "totalFolios", totalSeguro,
                    "mensaje", "El archivador actualmente tiene " + totalSeguro + " folios acumulados."
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al consultar folios: " + e.getMessage()));
        }
    }

    @GetMapping("/ultimo-nro-orden")
    public ResponseEntity<?> obtenerUltimoNro(@RequestParam Integer idArchivador) {
        try {
            Integer ultimo = repository.obtenerUltimoNroOrden(idArchivador);
            int ultimoSeguro = (ultimo != null) ? ultimo : 0;

            return ResponseEntity.ok(Map.of(
                    "idArchivador", idArchivador,
                    "ultimoNroOrden", ultimoSeguro,
                    "siguienteNroOrden", (ultimoSeguro + 1)
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    @Transactional // Importante para la consistencia al calcular el nroOrden
    public ResponseEntity<?> crear(@RequestBody Map<String, Object> payload) {
        try {
            Documento doc = new Documento();

            // Validaciones previas al mapeo pesado
            Integer idArchivador = convertToInt(payload.get("idArchivador"));
            if (idArchivador == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Debe seleccionar un archivador."));
            }

            mapearDocumento(doc, payload);

            // 1. Validación de Usuario Obligatorio
            if (doc.getIdUsuario() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "El campo idUsuario es obligatorio."));
            }

            // 2. Validación de Duplicados de Código
            if (doc.getNumeroDocumentoOCodigoDocumento() != null &&
                    repository.existeNumeroDoc(doc.getNumeroDocumentoOCodigoDocumento())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Ese número de documento ya existe."));
            }

            // 3. Lógica de Nro Orden Robusta
            // Si el front no lo manda o manda 0, calculamos el máximo actual + 1
            if (doc.getNroOrden() == null || doc.getNroOrden() == 0) {
                Integer ultimo = repository.obtenerUltimoNroOrden(idArchivador);
                doc.setNroOrden((ultimo != null ? ultimo : 0) + 1);
            }

            doc.setFechaRegistro(LocalDate.now());
            if (doc.getEstado() == null) doc.setEstado(1);

            Documento guardado = repository.save(doc);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "mensaje", "Documento creado correctamente",
                    "id", guardado.getId(),
                    "nroOrden", guardado.getNroOrden()
            ));
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Formato de fecha inválido. Use YYYY-MM-DD"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error al crear: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> editar(@PathVariable Integer id, @RequestBody Map<String, Object> payload) {
        return repository.findById(id).map(doc -> {
            try {
                mapearDocumento(doc, payload);
                repository.save(doc);
                return ResponseEntity.ok(Map.of("mensaje", "Documento actualizado con éxito"));
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body(Map.of("error", "Error al actualizar: " + e.getMessage()));
            }
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Documento no encontrado")));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        Optional<Documento> docOpt = repository.findById(id);
        if (docOpt.isPresent()) {
            Documento doc = docOpt.get();
            try {
                repository.delete(doc);
                return ResponseEntity.ok(Map.of("mensaje", "Eliminado físicamente del sistema"));
            } catch (Exception e) {
                // Si hay FK que impiden el borrado, hacemos borrado lógico
                doc.setEstado(0);
                repository.save(doc);
                return ResponseEntity.ok(Map.of("mensaje", "El registro tiene dependencias. Se ha desactivado (Borrado Lógico)"));
            }
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Centraliza la lógica de conversión de datos del Map al Objeto
     */
    private void mapearDocumento(Documento doc, Map<String, Object> p) {
        doc.setAsunto(getString(p.get("asunto"), doc.getAsunto()));
        doc.setReferencia(getString(p.get("referencia"), doc.getReferencia()));
        doc.setRutaArchivo(getString(p.get("rutaArchivo"), doc.getRutaArchivo()));
        doc.setObservacionRevision(getString(p.get("observacionRevision"), doc.getObservacionRevision()));
        doc.setNumeroDocumentoOCodigoDocumento(getString(p.get("numeroDocumentoOCodigoDocumento"), doc.getNumeroDocumentoOCodigoDocumento()));

        // Mapeo de IDs
        doc.setIdTipoDocumento(convertToInt(p.get("idTipoDocumento")));
        doc.setIdArchivador(convertToInt(p.get("idArchivador")));
        doc.setIdDependencia(convertToInt(p.get("idDependencia")));
        doc.setIdUsuario(convertToInt(p.get("idUsuario")));
        doc.setIdUsuarioModifica(convertToInt(p.get("idUsuarioModifica")));

        // Mapeo de campos específicos
        doc.setNroOrden(convertToInt(p.get("nroOrden")));
        doc.setNumero_Folio(convertToInt(p.get("Numero_Folio"))); // Manteniendo tu nomenclatura
        doc.setEstado(convertToInt(p.getOrDefault("estado", doc.getEstado())));

        // Lógica para Boolean/TinyInt
        Object valiosoObj = p.get("esValioso");
        if (valiosoObj instanceof Boolean) {
            doc.setEsValioso((Boolean) valiosoObj ? 1 : 0);
        } else if (valiosoObj != null) {
            doc.setEsValioso(convertToInt(valiosoObj));
        }

        // Fecha con manejo de errores
        if (p.get("fechaDocumento") != null && !p.get("fechaDocumento").toString().isEmpty()) {
            doc.setFechaDocumento(LocalDate.parse(p.get("fechaDocumento").toString()));
        }
    }

    private String getString(Object obj, String defaultValue) {
        return (obj != null) ? obj.toString() : defaultValue;
    }

    private Integer convertToInt(Object obj) {
        if (obj == null || obj.toString().trim().isEmpty()) return null;
        try {
            if (obj instanceof Number) return ((Number) obj).intValue();
            return Integer.parseInt(obj.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}