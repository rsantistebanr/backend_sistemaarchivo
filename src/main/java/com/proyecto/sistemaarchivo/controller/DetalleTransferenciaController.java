package com.proyecto.sistemaarchivo.controller;

import com.proyecto.sistemaarchivo.JWT.JwtUtils;
import com.proyecto.sistemaarchivo.model.Archivador;
import com.proyecto.sistemaarchivo.model.DetalleTransferencia;
import com.proyecto.sistemaarchivo.repository.ArchivadorRepository;
import com.proyecto.sistemaarchivo.repository.DetalleTransferenciaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/detalletransferencia")
@CrossOrigin(origins = "*")
public class DetalleTransferenciaController {

    @Autowired
    private DetalleTransferenciaRepository repository;

    @Autowired
    private ArchivadorRepository archivadorRepository;

    @Autowired
    private JwtUtils jwtUtils;

    // Obtener todos los archivadores/documentos de una transferencia específica
    @GetMapping("/transferencia/{idTransferencia}")
    public ResponseEntity<?> listarPorTransferencia(@PathVariable Integer idTransferencia, HttpServletRequest request) {
        List<DetalleTransferencia> detalles = repository.findByIdTransferencia(idTransferencia);

        if (esPrivilegiado(request)) {
            return ResponseEntity.ok(detalles);
        }

        Integer idDepToken = obtenerDependenciaToken(request);
        System.out.println("La dependencia es: "+idDepToken);
        if (idDepToken == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No se pudo determinar la dependencia del usuario"));
        }

        List<DetalleTransferencia> filtrados = detalles.stream().filter(d -> {
            Archivador arc = archivadorRepository.findById(d.getIdArchivador()).orElse(null);
            return arc != null && idDepToken.equals(arc.getIdDependencia());
        }).toList();

        return ResponseEntity.ok(filtrados);
    }

    // Ver un detalle específico
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Integer id, HttpServletRequest request) {
        return repository.findById(id).map(detalle -> {
            if (esPrivilegiado(request)) {
                return ResponseEntity.ok(detalle);
            }

            Integer idDepToken = obtenerDependenciaToken(request);
            Archivador arc = archivadorRepository.findById(detalle.getIdArchivador()).orElse(null);
            if (idDepToken == null || arc == null || !idDepToken.equals(arc.getIdDependencia())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No tienes permiso para ver este detalle"));
            }
            return ResponseEntity.ok(detalle);
        }).orElse(ResponseEntity.notFound().build());
    }

    private boolean esPrivilegiado(HttpServletRequest request) {
        String rol = obtenerRolToken(request);
        System.out.println("ROL DETALLE: " + obtenerRolToken(request));
        System.out.println("ID DEP TOKEN DETALLE: " + obtenerDependenciaToken(request));
        System.out.println("AUTH HEADER DETALLE: " + request.getHeader("Authorization"));

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
