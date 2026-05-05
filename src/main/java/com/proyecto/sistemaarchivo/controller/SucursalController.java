package com.proyecto.sistemaarchivo.controller;

import com.proyecto.sistemaarchivo.JWT.JwtUtils;
import com.proyecto.sistemaarchivo.model.Sucursal;
import com.proyecto.sistemaarchivo.repository.SucursalRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

    @Autowired
    private JwtUtils jwtUtils;

    @GetMapping
    public ResponseEntity<?> listar(HttpServletRequest request) {
        if (esPrivilegiado(request)) {
            return ResponseEntity.ok(repository.findAll());
        }

        Integer idSucToken = obtenerSucursalToken(request);
        if (idSucToken == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No se pudo determinar la sucursal del usuario"));
        }

        return repository.findById(idSucToken)
                .<ResponseEntity<?>>map(s -> ResponseEntity.ok(List.of(s)))
                .orElse(ResponseEntity.ok(List.of()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Integer id, HttpServletRequest request) {
        if (!esPrivilegiado(request)) {
            Integer idSucToken = obtenerSucursalToken(request);
            if (idSucToken == null || !idSucToken.equals(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No tienes permiso para consultar esta sucursal"));
            }
        }

        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Sucursal obj, HttpServletRequest request) {
        try {
            if (!esPrivilegiado(request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No tienes permiso para crear sucursales"));
            }

            if (obj.getEstado() == null) obj.setEstado(true);

            repository.save(obj);
            return ResponseEntity.ok(Map.of("mensaje", "Sucursal creada con éxito"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable Integer id, @RequestBody Sucursal detalles, HttpServletRequest request) {
        if (!esPrivilegiado(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No tienes permiso para editar sucursales"));
        }

        return repository.findById(id).map(obj -> {
            obj.setNombre(detalles.getNombre());
            obj.setDireccion(detalles.getDireccion());

            if (detalles.getEstado() != null) {
                obj.setEstado(detalles.getEstado());
            }

            repository.save(obj);
            return ResponseEntity.ok(Map.of("mensaje", "Sucursal actualizada correctamente"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id, HttpServletRequest request) {
        if (!esPrivilegiado(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No tienes permiso para eliminar sucursales"));
        }

        try {
            repository.deleteById(id);
            return ResponseEntity.ok(Map.of("mensaje", "Sucursal eliminada físicamente"));

        } catch (Exception e) {
            return repository.findById(id).map(obj -> {
                obj.setEstado(false);
                repository.save(obj);
                return ResponseEntity.ok(Map.of(
                        "mensaje", "Sucursal desactivada (tenía registros asociados y no se pudo borrar)"
                ));
            }).orElse(ResponseEntity.notFound().build());
        }
    }

    @GetMapping("/buscar")
    public ResponseEntity<?> buscar(@RequestParam String criterio, HttpServletRequest request) {
        if (esPrivilegiado(request)) {
            return ResponseEntity.ok(repository.buscarPorCriterio(criterio));
        }

        Integer idSucToken = obtenerSucursalToken(request);
        if (idSucToken == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No se pudo determinar la sucursal del usuario"));
        }

        return repository.findById(idSucToken)
                .<ResponseEntity<?>>map(s -> ResponseEntity.ok(List.of(s)))
                .orElse(ResponseEntity.ok(List.of()));
    }

    private boolean esPrivilegiado(HttpServletRequest request) {
        String rol = obtenerRolToken(request);
        return "ADMINISTRADOR".equalsIgnoreCase(rol) || "USUARIOA".equalsIgnoreCase(rol);
    }

    private String obtenerRolToken(HttpServletRequest request) {
        String token = extraerToken(request);
        if (token == null) return null;
        try {
            return jwtUtils.obtenerRolDelToken(token);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer obtenerSucursalToken(HttpServletRequest request) {
        String token = extraerToken(request);
        if (token == null) return null;
        try {
            return jwtUtils.obtenerSucursalDelToken(token);
        } catch (Exception e) {
            return null;
        }
    }

    private String extraerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return null;
        return header.substring(7);
    }
}