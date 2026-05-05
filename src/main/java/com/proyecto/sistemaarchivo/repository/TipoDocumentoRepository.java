package com.proyecto.sistemaarchivo.repository;

import com.proyecto.sistemaarchivo.model.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TipoDocumentoRepository extends JpaRepository<TipoDocumento, Integer> {

    // Buscar por nombre
    @Query("SELECT t FROM TipoDocumento t WHERE LOWER(t.nombre) LIKE LOWER(CONCAT('%', :nombre, '%'))")
    List<TipoDocumento> buscarPorNombre(@Param("nombre") String nombre);

    List<TipoDocumento> findByEstadoTrue();
}
