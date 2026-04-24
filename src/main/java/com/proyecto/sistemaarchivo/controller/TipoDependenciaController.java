package com.proyecto.sistemaarchivo.controller;

import com.proyecto.sistemaarchivo.model.TipoDependencia;
import com.proyecto.sistemaarchivo.repository.TipoDependenciaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tipodependencias")
@CrossOrigin(origins = "*")
public class TipoDependenciaController {

    @Autowired
    private TipoDependenciaRepository repository;

    // 1. LISTAR TODOS
    @GetMapping
    public List<TipoDependencia> listar() {
        return repository.findAll();
    }

    // 2. OBTENER POR ID
    @GetMapping("/{id}")
    public ResponseEntity<TipoDependencia> obtenerPorId(@PathVariable Integer id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 3. CREAR
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody TipoDependencia tipo) {
        try {
            repository.save(tipo);
            return ResponseEntity.ok(Map.of("mensaje", "Tipo de Dependencia creado con éxito"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // 4. EDITAR
    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable Integer id, @RequestBody TipoDependencia detalles) {
        return repository.findById(id).map(tipo -> {
            tipo.setNombre(detalles.getNombre());
            repository.save(tipo);
            return ResponseEntity.ok(Map.of("mensaje", "Tipo de Dependencia actualizado correctamente"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // 5. ELIMINAR
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        return repository.findById(id).map(tipo -> {
            try {
                repository.delete(tipo);
                return ResponseEntity.ok(Map.of("mensaje", "Tipo de Dependencia eliminado físicamente"));
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "No se puede eliminar el tipo porque está siendo usado por una o más dependencias."
                ));
            }
        }).orElse(ResponseEntity.notFound().build());
    }
}