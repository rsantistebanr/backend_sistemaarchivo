package com.proyecto.sistemaarchivo.repository;

import com.proyecto.sistemaarchivo.model.SucursalDependencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Repository
public interface SucursalDependenciaRepository extends JpaRepository<SucursalDependencia, Integer> {

    // Para el EDITAR: Busca el vínculo actual de la dependencia
    Optional<SucursalDependencia> findByIdDependencia(Integer idDependencia);

    // Para el ELIMINAR: Borra el vínculo de la tabla intermedia
    @Transactional
    void deleteByIdDependencia(Integer idDependencia);
}

