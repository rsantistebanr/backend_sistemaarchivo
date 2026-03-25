package com.proyecto.sistemaarchivo.controller;

import com.proyecto.sistemaarchivo.model.TipoDocumento;
import com.proyecto.sistemaarchivo.repository.TipoDocumentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tipodocumento")
@CrossOrigin(origins = "*")
public class TipoDocumentoController {

    @Autowired
    private TipoDocumentoRepository repository;

    @GetMapping
    public List<TipoDocumento> listar() {
        return repository.findAll();
    }

    @GetMapping("/activos")
    public List<TipoDocumento> listarActivos() {
        return repository.findByEstadoTrue();
    }

    @GetMapping("/buscar")
    public List<TipoDocumento> buscar(@RequestParam String nombre) {
        return repository.buscarPorNombre(nombre);
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody TipoDocumento tipo) {
        try {
            if (tipo.getEstado() == null) tipo.setEstado(true);
            return ResponseEntity.ok(repository.save(tipo));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable Integer id, @RequestBody TipoDocumento detalles) {
        return repository.findById(id).map(tipo -> {
            tipo.setNombre(detalles.getNombre());
            tipo.setEstado(detalles.getEstado());
            repository.save(tipo);
            return ResponseEntity.ok(Map.of("mensaje", "Tipo de documento actualizado"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        return repository.findById(id).map(tipo -> {
            try {
                // Borrado físico si no hay documentos usando este tipo
                repository.delete(tipo);
                return ResponseEntity.ok(Map.of("mensaje", "Eliminado físicamente"));
            } catch (Exception e) {
                // Borrado lógico si ya tiene documentos asociados
                tipo.setEstado(false);
                repository.save(tipo);
                return ResponseEntity.ok(Map.of("mensaje", "Desactivado (tiene documentos asociados)"));
            }
        }).orElse(ResponseEntity.notFound().build());
    }
}
