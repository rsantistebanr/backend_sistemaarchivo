package com.proyecto.sistemaarchivo.controller;

import com.proyecto.sistemaarchivo.model.HistorialRevision;
import com.proyecto.sistemaarchivo.repository.HistorialRevisionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/historial-revision")
@CrossOrigin(origins = "*")
public class HistorialRevisionController {

    @Autowired
    private HistorialRevisionRepository repository;

    @GetMapping
    public List<HistorialRevision> listarTodo() {
        return repository.findAll();
    }

    // Ver el historial de un ítem específico del envío
    @GetMapping("/detalle/{idDetalle}")
    public List<HistorialRevision> listarPorDetalle(@PathVariable Integer idDetalle) {
        return repository.findByIdDetalleEnvio(idDetalle);
    }

    // Endpoint para el revisor (Conforme/Rechazado)
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarRevision(@PathVariable Integer id, @RequestBody Map<String, Object> payload) {
        return repository.findById(id).map(historial -> {

            // 1: rechazado, 2: conforme, 3: pendiente (según tu DER)
            if (payload.containsKey("estado")) {
                historial.setEstado((Integer) payload.get("estado"));
            }

            historial.setIdUsuarioRevision((Integer) payload.get("idUsuarioRevision"));
            historial.setFecha_Revision(LocalDateTime.now());

            // Si el revisor pone una fecha de subsanación (cuando es rechazado)
            if (payload.containsKey("fechaSubSancion")) {
                historial.setFecha_SubSancion(LocalDateTime.now());
            }

            repository.save(historial);
            return ResponseEntity.ok(Map.of("mensaje", "Revisión registrada correctamente"));
        }).orElse(ResponseEntity.notFound().build());
    }
}
