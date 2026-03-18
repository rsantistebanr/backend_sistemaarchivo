package com.proyecto.sistemaarchivo.controller;

import com.proyecto.sistemaarchivo.model.Sucursal;
import com.proyecto.sistemaarchivo.repository.SucursalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sucursales")
@CrossOrigin(origins = "*")
public class SucursalController {

    @Autowired
    private SucursalRepository repository;

    @GetMapping
    public List<Sucursal> listar() {
        return repository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Sucursal obj) {
        try {
            // Si el estado viene nulo, por defecto es true (activo)
            if (obj.getEstado() == null) obj.setEstado(true);

            repository.save(obj);
            return ResponseEntity.ok(Map.of("mensaje", "Sucursal creada con éxito"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable Integer id, @RequestBody Sucursal detalles) {
        return repository.findById(id).map(obj -> {
            obj.setNombre(detalles.getNombre());
            obj.setDireccion(detalles.getDireccion());

            // Validamos que el estado no sea nulo al editar para no romper la lógica
            if (detalles.getEstado() != null) {
                obj.setEstado(detalles.getEstado());
            }

            repository.save(obj);
            return ResponseEntity.ok(Map.of("mensaje", "Sucursal actualizada correctamente"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        try {
            // 1. Intento de borrado físico (Si no tiene hijos/FK, funcionará)
            repository.deleteById(id);
            return ResponseEntity.ok(Map.of("mensaje", "Sucursal eliminada físicamente"));

        } catch (Exception e) {
            // 2. Si hay registros asociados (Usuarios o Dependencias), hacemos Borrado Lógico
            return repository.findById(id).map(obj -> {
                obj.setEstado(false); // Cambiamos a false (0 en la BD)
                repository.save(obj);
                return ResponseEntity.ok(Map.of(
                        "mensaje", "Sucursal desactivada (tenía registros asociados y no se pudo borrar)"
                ));
            }).orElse(ResponseEntity.notFound().build());
        }
    }
    // NUEVO: Endpoint de búsqueda para el filtro
    @GetMapping("/buscar")
    public List<Sucursal> buscar(@RequestParam String criterio) {
        return repository.buscarPorCriterio(criterio);
    }
}