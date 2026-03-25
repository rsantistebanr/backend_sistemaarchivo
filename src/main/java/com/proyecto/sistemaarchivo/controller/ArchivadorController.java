package com.proyecto.sistemaarchivo.controller;

import com.proyecto.sistemaarchivo.model.Archivador;
import com.proyecto.sistemaarchivo.repository.ArchivadorRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/archivadores")
@CrossOrigin(origins = "*")
public class ArchivadorController {

    @Autowired
    private ArchivadorRepository repository;

    @GetMapping("/buscar")
    public ResponseEntity<?> buscar(
            @RequestParam(required = false) Integer idDependencia,
            @RequestParam(required = false) Integer idTipoArchivador,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer esValioso) {

        List<Map<String, Object>> resultados = repository.filtrarArchivadoresPro(
                idDependencia,
                idTipoArchivador,
                anio,
                esValioso
        );

        return ResponseEntity.ok(resultados);
    }

    @GetMapping
    public ResponseEntity<?> listar() {
        return ResponseEntity.ok(repository.obtenerDetalleCompleto(null));
    }

    // CREAR ARCHIVADOR
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Map<String, Object> payload) {
        try {
            Archivador arc = new Archivador();

            arc.setIdEstante(convertToInt(payload.get("idEstante")));
            arc.setIdDependencia(convertToInt(payload.get("idDependencia")));
            arc.setIdTipoArchivador(convertToInt(payload.get("idTipoArchivador")));
            arc.setAño(convertToInt(payload.get("año")));
            arc.setNumero(convertToInt(payload.get("numero")));
            arc.setCantidad_folio(convertToInt(payload.get("cantidad_folio")));
            arc.setCantidadDoc(convertToInt(payload.get("cantidadDoc")));
            arc.setDocumentoInicio(convertToInt(payload.get("documentoInicio")));
            arc.setDocumentoFin(convertToInt(payload.get("documentoFin")));
            arc.setEs_valioso(convertToInt(payload.get("es_valioso")));

            // Ubicación exacta del archivador dentro del estante
            arc.setNumCuerpo(convertToInt(payload.get("numCuerpo")));
            arc.setValda(convertToValda(payload.get("valda")));

            if (payload.get("unidadMedida") != null) {
                arc.setUnidadMedida(convertToDouble(payload.get("unidadMedida")));
            }

            Archivador guardado = repository.save(arc);
            return ResponseEntity.ok(repository.obtenerDetalleCompleto(guardado.getId()));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // EDITAR ARCHIVADOR
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> editar(@PathVariable Integer id, @RequestBody Map<String, Object> payload) {
        return repository.findById(id).map(arc -> {
            actualizarCampos(arc, payload);
            repository.save(arc);

            // Retorna el detalle para que el frontend vea el cambio de Valda/Cuerpo al instante
            return ResponseEntity.ok(repository.obtenerDetalleCompleto(id));
        }).orElse(ResponseEntity.notFound().build());
    }

    // Lógica compartida para no repetir código
    private void actualizarCampos(Archivador arc, Map<String, Object> p) {
        arc.setIdEstante(convertToInt(p.getOrDefault("idEstante", arc.getIdEstante())));
        arc.setIdDependencia(convertToInt(p.getOrDefault("idDependencia", arc.getIdDependencia())));
        arc.setIdTipoArchivador(convertToInt(p.getOrDefault("idTipoArchivador", arc.getIdTipoArchivador())));
        arc.setAño(convertToInt(p.getOrDefault("año", arc.getAño())));
        arc.setNumero(convertToInt(p.getOrDefault("numero", arc.getNumero())));
        arc.setCantidad_folio(convertToInt(p.getOrDefault("cantidad_folio", arc.getCantidad_folio())));
        arc.setCantidadDoc(convertToInt(p.getOrDefault("cantidadDoc", arc.getCantidadDoc())));
        arc.setDocumentoInicio(convertToInt(p.getOrDefault("documentoInicio", arc.getDocumentoInicio())));
        arc.setDocumentoFin(convertToInt(p.getOrDefault("documentoFin", arc.getDocumentoFin())));
        arc.setEs_valioso(convertToInt(p.getOrDefault("es_valioso", arc.getEs_valioso())));

        // Ubicación exacta (si viene en payload)
        if (p.containsKey("numCuerpo")) {
            arc.setNumCuerpo(convertToInt(p.get("numCuerpo")));
        }
        if (p.containsKey("valda")) {
            arc.setValda(convertToValda(p.get("valda")));
        }

        if (p.containsKey("unidadMedida")) {
            arc.setUnidadMedida(convertToDouble(p.get("unidadMedida")));
        }
    }

    // ELIMINAR ARCHIVADOR
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        try {
            repository.deleteById(id);
            return ResponseEntity.ok(Map.of("mensaje", "Archivador eliminado del sistema"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se puede eliminar: tiene documentos o transferencias vinculadas"));
        }
    }

    // Helper para evitar errores de cast
    private Integer convertToInt(Object obj) {
        if (obj == null) return null;

        if (obj instanceof Number n) {
            return n.intValue();
        }

        if (obj instanceof String s) {
            String t = s.trim();
            if (t.isEmpty()) return null;
            return Integer.parseInt(t);
        }

        throw new IllegalArgumentException("Valor no convertible a Integer: " + obj);
    }

    private Double convertToDouble(Object obj) {
        if (obj == null) return null;

        if (obj instanceof Number n) {
            return n.doubleValue();
        }

        if (obj instanceof String s) {
            String t = s.trim();
            if (t.isEmpty()) return null;
            return Double.parseDouble(t);
        }

        throw new IllegalArgumentException("Valor no convertible a Double: " + obj);
    }

    private String convertToValda(Object obj) {
        if (obj == null) return null;

        String texto = String.valueOf(obj).trim().toUpperCase();
        if (texto.isEmpty()) return null;

        // Guardar solo la primera letra válida
        char c = texto.charAt(0);
        if (c < 'A' || c > 'Z') {
            throw new IllegalArgumentException("Valda inválida: " + obj);
        }

        return String.valueOf(c);
    }
}
