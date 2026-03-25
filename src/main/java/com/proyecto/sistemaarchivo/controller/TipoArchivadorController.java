package com.proyecto.sistemaarchivo.controller;

import com.proyecto.sistemaarchivo.model.TipoArchivador;
import com.proyecto.sistemaarchivo.repository.TipoArchivadorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tipoarchivadores")
@CrossOrigin(origins = "*")
public class TipoArchivadorController {

    @Autowired
    private TipoArchivadorRepository repository;

    @GetMapping
    public List<TipoArchivador> listar() {
        return repository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody TipoArchivador tipo) {
        try {
            repository.save(tipo);
            return ResponseEntity.ok(Map.of("mensaje", "Tipo de Archivador creado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable Integer id, @RequestBody TipoArchivador detalles) {
        return repository.findById(id).map(tipo -> {
            tipo.setNombre(detalles.getNombre());
            tipo.setEstado(detalles.getEstado());
            repository.save(tipo);
            return ResponseEntity.ok(Map.of("mensaje", "Tipo de Archivador actualizado"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        try {
            repository.deleteById(id);
            return ResponseEntity.ok(Map.of("mensaje", "Tipo de Archivador eliminado"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se puede eliminar: existen archivadores físicos asociados a este tipo"));
        }
    }
}
