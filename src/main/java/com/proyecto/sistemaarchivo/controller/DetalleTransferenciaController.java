package com.proyecto.sistemaarchivo.controller;

import com.proyecto.sistemaarchivo.model.DetalleTransferencia;
import com.proyecto.sistemaarchivo.repository.DetalleTransferenciaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/detalletransferencia")
@CrossOrigin(origins = "*")
public class DetalleTransferenciaController {

    @Autowired
    private DetalleTransferenciaRepository repository;

    // Obtener todos los archivadores/documentos de una transferencia específica
    @GetMapping("/transferencia/{idTransferencia}")
    public List<DetalleTransferencia> listarPorTransferencia(@PathVariable Integer idTransferencia) {
        return repository.findByIdTransferencia(idTransferencia);
    }

    // Ver un detalle específico
    @GetMapping("/{id}")
    public ResponseEntity<DetalleTransferencia> obtenerPorId(@PathVariable Integer id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
