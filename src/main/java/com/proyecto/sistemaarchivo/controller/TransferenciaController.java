package com.proyecto.sistemaarchivo.controller;

import com.proyecto.sistemaarchivo.model.*;
import com.proyecto.sistemaarchivo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/transferencias")
@CrossOrigin(origins = "*")
public class TransferenciaController {

    @Autowired private TransferenciaRepository transRepo;
    @Autowired private DetalleTransferenciaRepository detalleRepo;
    @Autowired private HistorialRevisionRepository historialRepo;
    @Autowired private ArchivadorRepository archivadorRepository;

    @PostMapping
    @Transactional
    public ResponseEntity<?> crearTransferencia(@RequestBody Map<String, Object> payload) {
        try {
            List<Integer> idsArchivadores = (List<Integer>) payload.get("idsArchivadores");
            if (idsArchivadores == null || idsArchivadores.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Debe seleccionar archivadores."));
            }

            // 1. Cálculo de Metros (0.08 por archivador según DER)
            List<Archivador> archivadoresBD = archivadorRepository.findAllById(idsArchivadores);
            Double sumaML = archivadoresBD.stream()
                    .mapToDouble(a -> a.getUnidadMedida() != null ? a.getUnidadMedida() : 0.0)
                    .sum();

            // 2. Guardar Transferencia (Cabecera)
            Transferencia t = new Transferencia();
            t.setIdUsuarioEnvio(convertToInt(payload.get("idUsuarioEnvio")));
            t.setIdDependenciaDestino(convertToInt(payload.get("idDependenciaDestino")));
            t.setObservacion((String) payload.getOrDefault("observacion", "Envío de archivadores"));
            t.setMetrosLineales(sumaML);
            t.setCantidadArchivadores(archivadoresBD.size());
            t.setFechaTransferencia(LocalDateTime.now());

            // AGREGADO: IdUsuarioRecepcion (Se captura del payload si ya se sabe quién recibe, sino queda null)
            t.setIdUsuarioRecepcion(convertToInt(payload.get("idUsuarioRecepcion")));

            Transferencia guardada = transRepo.save(t);

            // 3. Guardar Detalles
            for (Archivador arc : archivadoresBD) {
                DetalleTransferencia dt = new DetalleTransferencia();
                dt.setIdTransferencia(guardada.getId());
                dt.setIdArchivador(arc.getId());
                // Si vienes de una carga masiva de documentos, aquí podrías asignar el IdDocumentoExterno
                detalleRepo.save(dt);
            }

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Transferencia registrada con éxito",
                    "Metros Lineales", sumaML,
                    "idTransferencia", guardada.getId()
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // Para evitar el error de "MissingServletRequestParameter",
    // asegúrate de que el parámetro sea opcional o venía de otra lógica
    @PutMapping("/revisar/{idDetalleEnvio}")
    @Transactional
    public ResponseEntity<?> revisarArchivador(
            @PathVariable Integer idDetalleEnvio,
            @RequestBody Map<String, Object> body) {
        try {
            List<HistorialRevision> lista = historialRepo.findByIdDetalleEnvio(idDetalleEnvio);

            HistorialRevision h = lista.isEmpty() ? new HistorialRevision() : lista.get(0);

            h.setIdDetalleEnvio(idDetalleEnvio);
            h.setEstado(convertToInt(body.get("estado")));
            h.setIdUsuarioRevision(convertToInt(body.get("idUsuarioRevision")));
            h.setFecha_Revision(LocalDateTime.now());

            if (body.get("subsanar") != null && (boolean) body.get("subsanar")) {
                h.setFecha_SubSancion(LocalDateTime.now());
            }

            historialRepo.save(h);
            return ResponseEntity.ok(Map.of("mensaje", "Revisión procesada correctamente"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    private Integer convertToInt(Object obj) {
        if (obj == null || obj.toString().trim().isEmpty()) return null;
        try {
            if (obj instanceof Number) return ((Number) obj).intValue();
            return Integer.parseInt(obj.toString().trim());
        } catch (Exception e) { return null; }
    }
}