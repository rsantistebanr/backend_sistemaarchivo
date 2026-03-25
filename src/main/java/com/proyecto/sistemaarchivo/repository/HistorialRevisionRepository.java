package com.proyecto.sistemaarchivo.repository;

import com.proyecto.sistemaarchivo.model.HistorialRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HistorialRevisionRepository extends JpaRepository<HistorialRevision, Integer> {
    List<HistorialRevision> findByIdDetalleEnvio(Integer idDetalleEnvio);
}
