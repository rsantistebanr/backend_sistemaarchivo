package com.proyecto.sistemaarchivo.repository;

import com.proyecto.sistemaarchivo.model.Caja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CajaRepository extends JpaRepository<Caja, Integer> {
    Optional<Caja> findByNroCaja(String nroCaja);
    boolean existsByNroCaja(String nroCaja);
    List<Caja> findByIdDependencia(Integer idDependencia);
    List<Caja> findByIdTipoDocumento(Integer idTipoDocumento);
    List<Caja> findByIdDependenciaAndIdTipoDocumento(Integer idDependencia, Integer idTipoDocumento);
}

