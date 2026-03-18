package com.proyecto.sistemaarchivo.controller;

import com.proyecto.sistemaarchivo.model.Rol;
import com.proyecto.sistemaarchivo.repository.RolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/roles")
@CrossOrigin(origins = "*")
public class RolController {

    @Autowired
    private RolRepository rolRepository;

    @GetMapping
    public List<Rol> listarRoles() {
        return rolRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Rol rol) {
        rolRepository.save(rol);
        return ResponseEntity.ok(Map.of("mensaje", "Rol creado con éxito"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable Integer id, @RequestBody Rol rolDetalles) {
        return rolRepository.findById(id).map(rol -> {
            rol.setNombre(rolDetalles.getNombre());
            rol.setDescripcion(rolDetalles.getDescripcion());
            rolRepository.save(rol);
            return ResponseEntity.ok(Map.of("mensaje", "Rol actualizado con éxito"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        try {
            return rolRepository.findById(id).map(rol -> {
                rolRepository.delete(rol);
                return ResponseEntity.ok(Map.of("mensaje", "Rol eliminado con éxito"));
            }).orElse(ResponseEntity.notFound().build());
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Este error ocurre si hay usuarios usando este Rol (Foreign Key Constraint)
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "No se puede eliminar el rol porque hay usuarios asignados a él."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al eliminar el rol: " + e.getMessage()));
        }
    }
}
