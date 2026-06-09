package com.proyecto.sistemaarchivo.controller;

import com.proyecto.sistemaarchivo.JWT.JwtUtils;
import com.proyecto.sistemaarchivo.model.Dependencia;
import com.proyecto.sistemaarchivo.model.SucursalDependencia;
import com.proyecto.sistemaarchivo.repository.DependenciaRepository;
import com.proyecto.sistemaarchivo.repository.SucursalDependenciaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dependencias")
@CrossOrigin(origins = "*")
public class DependenciaController {

    @Autowired
    private DependenciaRepository repository;

    @Autowired
    private SucursalDependenciaRepository sucursalDepRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @GetMapping
    public ResponseEntity<?> listar(HttpServletRequest request) {
        if (esPrivilegiado(request)) {
            return ResponseEntity.ok(repository.findAll());
        }

        Integer idDepToken = obtenerDependenciaToken(request);
        if (idDepToken == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No se pudo determinar la dependencia del usuario"));
        }

        return repository.findById(idDepToken)
                .<ResponseEntity<?>>map(dep -> ResponseEntity.ok(List.of(dep)))
                .orElse(ResponseEntity.ok(List.of()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Integer id, HttpServletRequest request) {
        if (!esPrivilegiado(request)) {
            Integer idDepToken = obtenerDependenciaToken(request);
            if (idDepToken == null || !idDepToken.equals(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No tienes permiso para consultar esta dependencia"));
            }
        }

        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // FILTRO
    @GetMapping("/filtrar")
    public ResponseEntity<?> filtrar(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) Integer idTipoDependencia,
            @RequestParam(required = false) Integer idSucursal,
            @RequestParam(required = false) Boolean estado) {
        try {
            List<Dependencia> resultados = repository.filtrarAvanzado(
                    nombre, color, idTipoDependencia, idSucursal, estado);
            return ResponseEntity.ok(resultados);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/sucursal/{idSucursal}")
    public List<Dependencia> listarPorSucursal(@PathVariable Integer idSucursal) {
        return repository.findBySucursalId(idSucursal);
    }

    //CREAR
    @PostMapping
    @Transactional
    public ResponseEntity<?> crear(@RequestBody Map<String, Object> payload) {
        try {
            List<Integer> idsSucursales = (List<Integer>) payload.get("idsSucursales");
            if (idsSucursales == null || idsSucursales.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Debe asignar al menos una sucursal"));
            }

            String codStr = (String) payload.get("codigoNumerico");
            if (codStr != null && !codStr.trim().isEmpty() && repository.existsByCodigoNumerico(codStr)) {
                return ResponseEntity.badRequest().body(Map.of("error", "El código '" + codStr + "' ya existe."));
            }

            Dependencia dep = new Dependencia();
            dep.setNombre((String) payload.get("nombre"));
            dep.setCodigoNumerico(codStr);
            dep.setTipoColor((String) payload.get("tipoColor"));
            dep.setColor((String) payload.get("color"));

            Number idTipoDep = (Number) payload.get("idTipoDependencia");
            dep.setIdTipoDependencia(idTipoDep != null ? idTipoDep.intValue() : null);

            dep.setFechaCreacion(LocalDateTime.now());
            dep.setEstado(true);
            dep.setFechaTermino(null);

            Dependencia depGuardada = repository.save(dep);

            for (Integer idSuc : idsSucursales) {
                SucursalDependencia relacion = new SucursalDependencia();
                relacion.setIdSucursal(idSuc);
                relacion.setIdDependencia(depGuardada.getId());
                relacion.setEstado(1.0);
                sucursalDepRepository.save(relacion);
            }

            return ResponseEntity.ok(Map.of("mensaje", "Dependencia '" + dep.getNombre() + "' creada con éxito"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    //Editar
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> editar(@PathVariable Integer id, @RequestBody Map<String, Object> payload) {
        return repository.findById(id).map(dep -> {
            try {
                if (payload.containsKey("nombre")) dep.setNombre((String) payload.get("nombre"));

                if (payload.containsKey("codigoNumerico")) {
                    String nuevoCod = (String) payload.get("codigoNumerico");
                    if (nuevoCod != null && !nuevoCod.equals(dep.getCodigoNumerico()) && repository.existsByCodigoNumerico(nuevoCod)) {
                        return ResponseEntity.badRequest().body(Map.of("error", "Código en uso"));
                    }
                    dep.setCodigoNumerico(nuevoCod);
                }

                if (payload.containsKey("tipoColor")) dep.setTipoColor((String) payload.get("tipoColor"));
                if (payload.containsKey("color")) dep.setColor((String) payload.get("color"));

                if (payload.containsKey("idTipoDependencia")) {
                    Number idTipo = (Number) payload.get("idTipoDependencia");
                    dep.setIdTipoDependencia(idTipo != null ? idTipo.intValue() : null);
                }

                if (payload.containsKey("estado")) {
                    Object edo = payload.get("estado");
                    Boolean nuevoEstado = edo instanceof Number ? ((Number) edo).intValue() != 0 : (Boolean) edo;

                    // Si se está desactivando ahora
                    if (dep.getEstado() && !nuevoEstado) {
                        dep.setFechaTermino(LocalDateTime.now());
                    }
                    // Si se está reactivando ahora
                    else if (!dep.getEstado() && nuevoEstado) {
                        dep.setFechaTermino(null);
                    }
                    dep.setEstado(nuevoEstado);
                }

                repository.save(dep);

                if (payload.containsKey("idsSucursales")) {
                    List<Integer> nuevosIds = (List<Integer>) payload.get("idsSucursales");
                    sucursalDepRepository.deleteByIdDependencia(id);
                    for (Integer idSuc : nuevosIds) {
                        SucursalDependencia rel = new SucursalDependencia();
                        rel.setIdSucursal(idSuc);
                        rel.setIdDependencia(id);
                        rel.setEstado(1.0);
                        sucursalDepRepository.save(rel);
                    }
                }

                return ResponseEntity.ok(Map.of("mensaje", "Actualizado correctamente"));
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    //Eliminar
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        return repository.findById(id).map(dep -> {
            try {
                // Borrado físico
                sucursalDepRepository.deleteByIdDependencia(id);
                repository.delete(dep);
                return ResponseEntity.ok(Map.of("mensaje", "Eliminado físicamente"));
            } catch (Exception e) {
                // Borrado lógico con FECHA DE TÉRMINO
                dep.setEstado(false);
                dep.setFechaTermino(LocalDateTime.now());
                repository.save(dep);
                return ResponseEntity.ok(Map.of("mensaje", "Desactivado. Se registró fecha de término."));
            }
        }).orElse(ResponseEntity.notFound().build());
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

    private Integer obtenerDependenciaToken(HttpServletRequest request) {
        String token = extraerToken(request);
        if (token == null) return null;
        try {
            return jwtUtils.obtenerDependenciaDelToken(token);
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