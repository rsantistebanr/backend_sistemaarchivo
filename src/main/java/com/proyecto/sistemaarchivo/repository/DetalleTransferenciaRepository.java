package com.proyecto.sistemaarchivo.repository;

import com.proyecto.sistemaarchivo.model.DetalleTransferencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DetalleTransferenciaRepository extends JpaRepository<DetalleTransferencia, Integer> {
    @Query(value = "SELECT * FROM detalle_tranferencia WHERE IdTransferencia = :idTransferencia", nativeQuery = true)
    List<DetalleTransferencia> findByIdTransferencia(@Param("idTransferencia") Integer idTransferencia);
}
