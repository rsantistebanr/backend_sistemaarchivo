package com.proyecto.sistemaarchivo.controller;

import com.proyecto.sistemaarchivo.model.Archivador;
import com.proyecto.sistemaarchivo.repository.ArchivadorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional; // Import correcto de Spring
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/archivadores")
@CrossOrigin(origins = "*")
public class ArchivadorController {

    @Autowired
    private ArchivadorRepository repository;

    // 1. LISTAR TODOS
    @GetMapping
    public ResponseEntity<?> listar() {
        return ResponseEntity.ok(repository.obtenerDetalleCompleto(null));
    }

    // 2. BUSCAR CON FILTROS
    @GetMapping("/buscar")
    public ResponseEntity<?> buscar(
            @RequestParam(required = false) Integer idDependencia,
            @RequestParam(required = false) Integer idTipoArchivador,
            @RequestParam(required = false) String anio,
            @RequestParam(required = false) Integer esValioso) {

        List<Map<String, Object>> resultados = repository.filtrarArchivadoresPro(
                idDependencia, idTipoArchivador, anio, esValioso
        );
        return ResponseEntity.ok(resultados);
    }

    // 3. OBTENER UNO POR ID (Fundamental para que el GET /id no falle)
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Integer id) {
        List<Map<String, Object>> detalle = repository.obtenerDetalleCompleto(id);
        if (detalle == null || detalle.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(detalle.get(0));
    }

    // 4. CREAR ARCHIVADOR
    @PostMapping
    @Transactional
    public ResponseEntity<?> crear(@RequestBody Map<String, Object> payload) {
        try {
            Archivador arc = new Archivador();
            actualizarCampos(arc, payload);

            Archivador guardado = repository.save(arc);

            // Retornamos el objeto completo con nombres de dependencia, etc.
            List<Map<String, Object>> detalle = repository.obtenerDetalleCompleto(guardado.getId());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(!detalle.isEmpty() ? detalle.get(0) : guardado);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al crear: " + e.getMessage()));
        }
    }

    // 5. EDITAR ARCHIVADOR
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> editar(@PathVariable Integer id, @RequestBody Map<String, Object> payload) {
        return repository.findById(id).map(arc -> {
            try {
                actualizarCampos(arc, payload);
                repository.save(arc);

                List<Map<String, Object>> detalle = repository.obtenerDetalleCompleto(id);
                return ResponseEntity.ok(!detalle.isEmpty() ? detalle.get(0) : arc);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Error al editar: " + e.getMessage()));
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    // 6. ELIMINAR ARCHIVADOR
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        try {
            repository.deleteById(id);
            return ResponseEntity.ok(Map.of("mensaje", "Archivador eliminado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No se puede eliminar: tiene registros vinculados."));
        }
    }

    // 7. VER DOCUMENTOS DE UN ARCHIVADOR
    @GetMapping("/{id}/documentos")
    public ResponseEntity<?> listarDocumentosPorArchivador(@PathVariable Integer id) {
        try {
            // Asumiendo que tu repository tiene un método para esto
            // Si no lo tiene, deberás crearlo en DocumentoRepository
            List<Map<String, Object>> documentos = repository.obtenerDocumentosPorArchivador(id);

            if (documentos.isEmpty()) {
                return ResponseEntity.ok(List.of()); // Retorna lista vacía si no hay nada
            }

            return ResponseEntity.ok(documentos);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al obtener documentos: " + e.getMessage()));
        }
    }

    // --- LÓGICA DE MAPEADO (Aquí está la magia del código/número) ---
    private void actualizarCampos(Archivador arc, Map<String, Object> p) {
        // Soporta que el front mande "codigo" o "numero"
        if (p.containsKey("codigo")) {
            arc.setNumero(convertToInt(p.get("codigo")));
        } else if (p.containsKey("numero")) {
            arc.setNumero(convertToInt(p.get("numero")));
        }

        arc.setIdEstante(convertToInt(p.getOrDefault("idEstante", arc.getIdEstante())));
        arc.setIdDependencia(convertToInt(p.getOrDefault("idDependencia", arc.getIdDependencia())));
        arc.setIdTipoArchivador(convertToInt(p.getOrDefault("idTipoArchivador", arc.getIdTipoArchivador())));

        // Manejo del año (soporta "año" y "anio" por si acaso)
        Object anioVal = p.get("año") != null ? p.get("año") : p.get("anio");
        if (anioVal != null) {
            arc.setAño(String.valueOf(anioVal));
        }

        arc.setCantidad_folio(convertToInt(p.getOrDefault("cantidad_folio", arc.getCantidad_folio())));
        arc.setCantidadDoc(convertToInt(p.getOrDefault("cantidadDoc", arc.getCantidadDoc())));
        arc.setDocumentoInicio(convertToInt(p.getOrDefault("documentoInicio", arc.getDocumentoInicio())));
        arc.setDocumentoFin(convertToInt(p.getOrDefault("documentoFin", arc.getDocumentoFin())));
        arc.setEs_valioso(convertToInt(p.getOrDefault("es_valioso", arc.getEs_valioso())));
        arc.setNumCuerpo(convertToInt(p.getOrDefault("numCuerpo", arc.getNumCuerpo())));
        arc.setValda(convertToValda(p.getOrDefault("valda", arc.getValda())));

        if (p.containsKey("unidadMedida")) {
            arc.setUnidadMedida(convertToDouble(p.get("unidadMedida")));
        }
    }

    // --- HELPERS ROBUSTOS ---
    private Integer convertToInt(Object obj) {
        if (obj == null || obj.toString().trim().isEmpty()) return null;
        try {
            if (obj instanceof Number n) return n.intValue();
            return Integer.parseInt(obj.toString().trim());
        } catch (Exception e) { return null; }
    }

    private Double convertToDouble(Object obj) {
        if (obj == null || obj.toString().trim().isEmpty()) return null;
        try {
            if (obj instanceof Number n) return n.doubleValue();
            return Double.parseDouble(obj.toString().trim());
        } catch (Exception e) { return null; }
    }

    private String convertToValda(Object obj) {
        if (obj == null) return null;
        String val = String.valueOf(obj).trim().toUpperCase();
        return val.isEmpty() ? null : String.valueOf(val.charAt(0));
    }
}