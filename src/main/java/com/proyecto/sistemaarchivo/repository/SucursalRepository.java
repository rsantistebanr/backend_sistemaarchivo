package com.proyecto.sistemaarchivo.repository;

import com.proyecto.sistemaarchivo.model.Sucursal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SucursalRepository extends JpaRepository<Sucursal, Integer> {

    // Búsqueda global por nombre o dirección
    @Query("SELECT s FROM Sucursal s WHERE " +
            "LOWER(s.nombre) LIKE LOWER(CONCAT('%', :criterio, '%')) OR " +
            "LOWER(s.direccion) LIKE LOWER(CONCAT('%', :criterio, '%'))")
    List<Sucursal> buscarPorCriterio(@Param("criterio") String criterio);

    // Para obtener solo sucursales activas (para los select del Front-end)
    List<Sucursal> findByEstadoTrue();
}
