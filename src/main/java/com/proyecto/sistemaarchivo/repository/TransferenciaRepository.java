package com.proyecto.sistemaarchivo.repository;

import com.proyecto.sistemaarchivo.dto.TransferenciaDTO;
import com.proyecto.sistemaarchivo.model.Transferencia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransferenciaRepository extends JpaRepository<Transferencia, Integer> {

    @Query("""
SELECT DISTINCT new com.proyecto.sistemaarchivo.dto.TransferenciaDTO(
    t.id,
    t.idUsuarioEnvio,
    t.idUsuarioRecepcion,
    t.idDependenciaDestino,
    t.observacion,
    t.fechaTransferencia,
    t.metrosLineales,
    t.cantidadArchivadores,
    h.estado
)
FROM Transferencia t
JOIN DetalleTransferencia d ON d.idTransferencia = t.id
JOIN HistorialRevision h ON h.idDetalleEnvio = d.id
WHERE (:estado IS NULL OR h.estado = :estado)
""")
    Page<TransferenciaDTO> findByEstado(
            @Param("estado") Integer estado,
            Pageable pageable);
}
