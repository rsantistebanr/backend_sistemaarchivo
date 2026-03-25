package com.proyecto.sistemaarchivo.repository;

import com.proyecto.sistemaarchivo.model.DetalleTransferencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DetalleTransferenciaRepository extends JpaRepository<DetalleTransferencia, Integer> {
    List<DetalleTransferencia> findByIdTransferencia(Integer idTransferencia);
}
