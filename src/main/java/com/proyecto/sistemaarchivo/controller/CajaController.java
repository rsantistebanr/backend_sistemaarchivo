package com.proyecto.sistemaarchivo.controller;

import com.proyecto.sistemaarchivo.JWT.JwtUtils;
import com.proyecto.sistemaarchivo.model.Caja;
import com.proyecto.sistemaarchivo.repository.CajaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cajas")
@CrossOrigin(origins = "*")
public class CajaController {

    @Autowired
    private CajaRepository repository;

    @Autowired
    private JwtUtils jwtUtils;

    @GetMapping
    public ResponseEntity<?> listar(
            @RequestParam(required = false) Integer idDependencia,
            @RequestParam(required = false) Integer idTipoDocumento,
            HttpServletRequest request) {
        if (!esPrivilegiado(request)) {
            Integer idDepToken = obtenerDependenciaToken(request);
            if (idDepToken == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No se pudo determinar la dependencia del usuario"));
            }
            if (idDependencia != null && !idDependencia.equals(idDepToken)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No puedes consultar cajas de otra dependencia"));
            }
            if (idTipoDocumento != null) {
                return ResponseEntity.ok(repository.findByIdDependenciaAndIdTipoDocumento(idDepToken, idTipoDocumento));
            }
            return ResponseEntity.ok(repository.findByIdDependencia(idDepToken));
        }

        if (idDependencia != null && idTipoDocumento != null) {
            return ResponseEntity.ok(repository.findByIdDependenciaAndIdTipoDocumento(idDependencia, idTipoDocumento));
        }
        if (idDependencia != null) {
            return ResponseEntity.ok(repository.findByIdDependencia(idDependencia));
        }
        if (idTipoDocumento != null) {
            return ResponseEntity.ok(repository.findByIdTipoDocumento(idTipoDocumento));
        }
        return ResponseEntity.ok(repository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Integer id, HttpServletRequest request) {
        return repository.findById(id).map(caja -> {
            if (!esPrivilegiado(request)) {
                Integer idDepToken = obtenerDependenciaToken(request);
                if (idDepToken == null || !idDepToken.equals(caja.getIdDependencia())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "No tienes permiso para ver esta caja"));
                }
            }
            return ResponseEntity.ok(caja);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> crear(@RequestBody Caja caja, HttpServletRequest request) {
        try {
            if (!esPrivilegiado(request)) {
                Integer idDepToken = obtenerDependenciaToken(request);
                if (idDepToken == null) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "No se pudo determinar la dependencia del usuario"));
                }
                if (caja.getIdDependencia() != null && !idDepToken.equals(caja.getIdDependencia())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "No puedes crear cajas para otra dependencia"));
                }
                caja.setIdDependencia(idDepToken);
            }

            String nroCaja = normalizar(caja.getNroCaja());
            if (nroCaja == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "nroCaja es obligatorio"));
            }
            if (repository.existsByNroCaja(nroCaja)) {
                return ResponseEntity.badRequest().body(Map.of("error", "El nroCaja ya existe"));
            }

            if (caja.getIdTipoDocumento() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "idTipoDocumento es obligatorio"));
            }

            caja.setNroCaja(nroCaja);
            Caja guardada = repository.save(caja);
            return ResponseEntity.status(HttpStatus.CREATED).body(guardada);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al crear caja: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> editar(@PathVariable Integer id, @RequestBody Caja payload, HttpServletRequest request) {
        return repository.findById(id).map(caja -> {
            try {
                if (!esPrivilegiado(request)) {
                    Integer idDepToken = obtenerDependenciaToken(request);
                    if (idDepToken == null || !idDepToken.equals(caja.getIdDependencia())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of("error", "No tienes permiso para editar esta caja"));
                    }
                    if (payload.getIdDependencia() != null && !idDepToken.equals(payload.getIdDependencia())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of("error", "No puedes mover la caja a otra dependencia"));
                    }
                    caja.setIdDependencia(idDepToken);
                }

                if (payload.getNroCaja() != null) {
                    String nroCaja = normalizar(payload.getNroCaja());
                    if (nroCaja == null) {
                        return ResponseEntity.badRequest().body(Map.of("error", "nroCaja es obligatorio"));
                    }

                    repository.findByNroCaja(nroCaja).ifPresent(existente -> {
                        if (!existente.getId().equals(id)) {
                            throw new IllegalArgumentException("El nroCaja ya existe");
                        }
                    });
                    caja.setNroCaja(nroCaja);
                }

                if (payload.getIdDependencia() != null) {
                    caja.setIdDependencia(payload.getIdDependencia());
                }

                if (payload.getIdTipoDocumento() != null) {
                    caja.setIdTipoDocumento(payload.getIdTipoDocumento());
                }

                if (payload.getObservacion() != null) {
                    caja.setObservacion(payload.getObservacion().trim());
                }

                return ResponseEntity.ok(repository.save(caja));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Error al editar caja: " + e.getMessage()));
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    private String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
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


