package com.proyecto.sistemaarchivo.repository;

import com.proyecto.sistemaarchivo.model.DocumentoExterno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DocumentoExternoRepository extends JpaRepository<DocumentoExterno, Integer> {

    Optional<DocumentoExterno> findByIdArchivadorAndNombreArchivo(Integer idArchivador, String nombreArchivo);

    boolean existsByIdArchivadorAndNombreArchivo(Integer idArchivador, String nombreArchivo);
}
