package com.proyecto.sistemaarchivo.repository;

import com.proyecto.sistemaarchivo.model.DocumentoExterno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentoExternoRepository extends JpaRepository<DocumentoExterno, Integer> {

    List<DocumentoExterno> findAllByOrderByFechaCargaDesc();

    List<DocumentoExterno> findByIdUsuarioOrderByFechaCargaDesc(Integer idUsuario);

    Optional<DocumentoExterno> findTopByNombreArchivoOrderByFechaCargaDesc(String nombreArchivo);

    Optional<DocumentoExterno> findByIdAndIdUsuario(Integer id, Integer idUsuario);

    // Para verificar si el nombre del archivo ya existe
    boolean existsByNombreArchivo(String nombreArchivo);
}