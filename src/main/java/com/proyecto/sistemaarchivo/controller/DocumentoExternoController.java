package com.proyecto.sistemaarchivo.controller;

import com.proyecto.sistemaarchivo.model.DocumentoExterno;
import com.proyecto.sistemaarchivo.repository.ArchivadorRepository;
import com.proyecto.sistemaarchivo.repository.DocumentoExternoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/documentoexterno")
@CrossOrigin(origins = "*")
public class DocumentoExternoController {

    @Autowired
    private DocumentoExternoRepository repository;

    @Autowired
    private ArchivadorRepository archivadorRepository;

    @PostMapping("/carga-masiva")
    @Transactional
    public ResponseEntity<?> cargaMasiva(
            @RequestParam Integer idArchivador,
            @RequestParam(required = false) Integer idUsuario,
            @RequestBody List<Map<String, Object>> payload) {
        try {
            if (!archivadorRepository.existsById(idArchivador)) {
                return ResponseEntity.badRequest().body(Map.of("error", "El archivador seleccionado no existe."));
            }

            if (payload == null || payload.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "La carga masiva no contiene filas."));
            }

            List<DocumentoExterno> documentosAGuardar = new ArrayList<>();
            List<Map<String, Object>> errores = new ArrayList<>();
            Set<String> nombresEnLote = new HashSet<>();
            int fila = 1;

            for (Map<String, Object> data : payload) {
                String nombreArchivo = convertToString(data.get("nombreArchivo"));
                String rutaArchivo = convertToString(data.get("rutaArchivo"));
                String formato = convertToString(data.get("formato"));
                Integer idUsuarioFila = firstNonNull(
                        convertToInt(data.get("idUsuario")),
                        convertToInt(data.get("IdUsuario")),
                        idUsuario
                );

                if (nombreArchivo == null) {
                    errores.add(Map.of("fila", fila, "error", "nombreArchivo es obligatorio"));
                    fila++;
                    continue;
                }

                String claveNombre = nombreArchivo.toLowerCase();
                if (!nombresEnLote.add(claveNombre)) {
                    errores.add(Map.of("fila", fila, "error", "nombreArchivo duplicado dentro del lote"));
                    fila++;
                    continue;
                }

                if (repository.existsByIdArchivadorAndNombreArchivo(idArchivador, nombreArchivo)) {
                    errores.add(Map.of("fila", fila, "error", "nombreArchivo ya existe en ese archivador"));
                    fila++;
                    continue;
                }

                DocumentoExterno doc = new DocumentoExterno();
                doc.setIdArchivador(idArchivador);
                doc.setIdUsuario(idUsuarioFila);
                doc.setNombreArchivo(nombreArchivo);
                doc.setRutaArchivo(rutaArchivo);
                doc.setFormato(formato);
                doc.setFechaCarga(LocalDateTime.now());
                doc.setEstado(true);

                documentosAGuardar.add(doc);
                fila++;
            }

            if (!documentosAGuardar.isEmpty()) {
                repository.saveAll(documentosAGuardar);
            }

            if (documentosAGuardar.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "mensaje", "No se guardaron registros por errores de validacion",
                        "idArchivador", idArchivador,
                        "creados", 0,
                        "rechazados", errores.size(),
                        "errores", errores
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Carga masiva completada",
                    "idArchivador", idArchivador,
                    "creados", documentosAGuardar.size(),
                    "rechazados", errores.size(),
                    "errores", errores
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error en carga masiva: " + e.getMessage()));
        }
    }

    private Integer convertToInt(Object obj) {
        if (obj == null) return null;
        try {
            if (obj instanceof Number) return ((Number) obj).intValue();
            return Integer.parseInt(obj.toString().trim());
        } catch (Exception e) { return null; }
    }

    private String convertToString(Object obj) {
        if (obj == null) return null;
        String valor = obj.toString().trim();
        return valor.isEmpty() ? null : valor;
    }

    private Integer firstNonNull(Integer... valores) {
        for (Integer valor : valores) {
            if (valor != null) return valor;
        }
        return null;
    }
}
