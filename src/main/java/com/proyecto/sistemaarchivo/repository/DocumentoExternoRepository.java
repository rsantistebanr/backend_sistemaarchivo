package com.proyecto.sistemaarchivo.repository;

import com.proyecto.sistemaarchivo.model.DocumentoExterno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DocumentoExternoRepository extends JpaRepository<DocumentoExterno, Integer> {

    // Spring entiende esto como: SELECT * FROM documentoexterno ORDER BY fechaCarga DESC
    List<DocumentoExterno> findAllByOrderByFechaCargaDesc();

    // Para verificar si el nombre del archivo ya existe
    boolean existsByNombreArchivo(String nombreArchivo);
}