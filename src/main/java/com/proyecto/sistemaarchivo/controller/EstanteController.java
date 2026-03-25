package com.proyecto.sistemaarchivo.controller;

import com.proyecto.sistemaarchivo.model.Estante;
import com.proyecto.sistemaarchivo.repository.EstanteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/estantes")
@CrossOrigin(origins = "*")
public class EstanteController {

    @Autowired
    private EstanteRepository repository;

    @GetMapping("/buscar")
    public List<Estante> buscar(
            @RequestParam(required = false) Integer idDep,
            @RequestParam(required = false) Integer numEstante,
            @RequestParam(required = false) Integer numCuerpo,
            @RequestParam(required = false) String valda) {
        return repository.filtrarEstantes(idDep, numEstante, numCuerpo, valda);
    }

    @GetMapping
    public List<Estante> listar() {
        return repository.findAll();
    }

    @GetMapping("/dependencia/{idDependencia}")
    public List<Estante> listarPorDependencia(@PathVariable Integer idDependencia) {
        return repository.findByIdDependencia(idDependencia);
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Map<String, Object> payload) {
        try {
            Estante estante = new Estante();
            estante.setIdDependencia(convertToInt(payload.get("idDependencia")));
            estante.setNum_Estante(convertToInt(payload.get("num_Estante")));
            estante.setNum_cuerpo(convertToInt(payload.get("num_cuerpo")));
            estante.setValda((String) payload.get("valda"));
            estante.setCantidad(convertToInt(payload.get("cantidad")));

            repository.save(estante);
            return ResponseEntity.ok(Map.of("mensaje", "Estante creado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable Integer id, @RequestBody Map<String, Object> payload) {
        return repository.findById(id).map(estante -> {
            if (payload.containsKey("idDependencia")) estante.setIdDependencia(convertToInt(payload.get("idDependencia")));
            if (payload.containsKey("num_Estante")) estante.setNum_Estante(convertToInt(payload.get("num_Estante")));
            if (payload.containsKey("num_cuerpo")) estante.setNum_cuerpo(convertToInt(payload.get("num_cuerpo")));
            if (payload.containsKey("valda")) estante.setValda((String) payload.get("valda"));
            if (payload.containsKey("cantidad")) estante.setCantidad(convertToInt(payload.get("cantidad")));

            repository.save(estante);
            return ResponseEntity.ok(Map.of("mensaje", "Estante actualizado"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        try {
            repository.deleteById(id);
            return ResponseEntity.ok(Map.of("mensaje", "Estante eliminado físicamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se puede eliminar: tiene archivadores asociados"));
        }
    }

    private Integer convertToInt(Object obj) {
        if (obj == null) return null;
        return ((Number) obj).intValue();
    }
}
