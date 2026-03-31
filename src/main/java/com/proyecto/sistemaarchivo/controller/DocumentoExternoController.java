package com.proyecto.sistemaarchivo.controller;

import com.proyecto.sistemaarchivo.model.DocumentoExterno;
import com.proyecto.sistemaarchivo.repository.DocumentoExternoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/documento-externo")
@CrossOrigin(origins = "*")
public class DocumentoExternoController {

    @Autowired
    private DocumentoExternoRepository repository;

    /**
     * LISTAR HISTORIAL:
     * Muestra todos los archivos Excel/Cargas masivas realizadas.
     * Es lo que el usuario ve para saber si su carga "EMITIDOS-OTIC-2024.xlsx" ya existe.
     */
    @GetMapping("/historial")
    public ResponseEntity<List<DocumentoExterno>> obtenerHistorial() {
        try {
            // Usa el método que ordena por fecha de carga descendente (lo más nuevo arriba)
            List<DocumentoExterno> historial = repository.findAllByOrderByFechaCargaDesc();
            return ResponseEntity.ok(historial);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * VERIFICAR DUPLICADOS:
     * El Front-end puede llamar a esto antes de subir para avisar al usuario
     * si el nombre del archivo ya fue usado.
     */
    @GetMapping("/verificar-nombre")
    public ResponseEntity<?> verificarNombre(@RequestParam String nombreArchivo) {
        boolean existe = repository.existsByNombreArchivo(nombreArchivo);
        return ResponseEntity.ok(Map.of("existe", existe));
    }

    /**
     * OBTENER DETALLE:
     * Por si necesitas ver los metadatos de una carga específica.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Integer id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * ELIMINAR REGISTRO DEL HISTORIAL:
     * OJO: Esto solo borra el "recuerdo" de la carga masiva en el historial.
     * No borra los documentos que se insertaron en la tabla Documento.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarHistorial(@PathVariable Integer id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return ResponseEntity.ok(Map.of("mensaje", "Registro eliminado del historial de cargas"));
        }
        return ResponseEntity.notFound().build();
    }
}