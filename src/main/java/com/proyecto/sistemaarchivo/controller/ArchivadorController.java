package com.proyecto.sistemaarchivo.controller;

import com.proyecto.sistemaarchivo.model.Archivador;
import com.proyecto.sistemaarchivo.repository.ArchivadorRepository;
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

    @GetMapping
    public List<Archivador> listar() {
        return repository.findAll();
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
            arc.setCantidadDoc(convertToInt(payload.get("cantidadDoc")));
            arc.setDocumentoInicio(convertToInt(payload.get("documentoInicio")));
            arc.setDocumentoFin(convertToInt(payload.get("documentoFin")));
            arc.setEs_valioso(convertToInt(payload.get("es_valioso")));

            Number unidad = (Number) payload.get("unidadMedida");
            arc.setUnidadMedida(unidad != null ? unidad.doubleValue() : 0.08);

            arc.setNumero(convertToInt(payload.get("numero")));
            arc.setCantidad_folio(convertToInt(payload.get("cantidad_folio")));

            repository.save(arc);
            return ResponseEntity.ok(Map.of("mensaje", "Archivador técnico registrado"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // EDITAR ARCHIVADOR
    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable Integer id, @RequestBody Map<String, Object> payload) {
        return repository.findById(id).map(arc -> {
            arc.setIdEstante(convertToInt(payload.getOrDefault("idEstante", arc.getIdEstante())));
            arc.setIdDependencia(convertToInt(payload.getOrDefault("idDependencia", arc.getIdDependencia())));
            arc.setIdTipoArchivador(convertToInt(payload.getOrDefault("idTipoArchivador", arc.getIdTipoArchivador())));
            arc.setAño(convertToInt(payload.getOrDefault("año", arc.getAño())));
            arc.setCantidadDoc(convertToInt(payload.getOrDefault("cantidadDoc", arc.getCantidadDoc())));
            arc.setDocumentoInicio(convertToInt(payload.getOrDefault("documentoInicio", arc.getDocumentoInicio())));
            arc.setDocumentoFin(convertToInt(payload.getOrDefault("documentoFin", arc.getDocumentoFin())));
            arc.setEs_valioso(convertToInt(payload.getOrDefault("es_valioso", arc.getEs_valioso())));

            if(payload.containsKey("unidadMedida")) {
                arc.setUnidadMedida(((Number) payload.get("unidadMedida")).doubleValue());
            }

            repository.save(arc);
            return ResponseEntity.ok(Map.of("mensaje", "Archivador actualizado correctamente"));
        }).orElse(ResponseEntity.notFound().build());
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
        return ((Number) obj).intValue();
    }
}
