package com.proyecto.sistemaarchivo.repository;

import com.proyecto.sistemaarchivo.model.Dependencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DependenciaRepository extends JpaRepository<Dependencia, Integer> {

    // 1. Verificar si el código ya existe (Tratado como String)
    boolean existsByCodigoNumerico(String codigoNumerico);

    // 2. Listado por Sucursal (Relación Muchos a Muchos / Tabla Intermedia)
    @Query(value = "SELECT d.* FROM dependencia d " +
            "JOIN sucursal_dependencia sd ON d.id = sd.IdDependencia " +
            "WHERE sd.IdSucursal = :idSucursal", nativeQuery = true)
    List<Dependencia> findBySucursalId(@Param("idSucursal") Integer idSucursal);

    // 3. Búsqueda Global (Para la barra de texto simple)
    @Query(value = "SELECT * FROM dependencia d WHERE " +
            "LOWER(d.nombre) LIKE LOWER(CONCAT('%', :criterio, '%')) OR " +
            "LOWER(d.codigoNumerico) LIKE LOWER(CONCAT('%', :criterio, '%')) OR " +
            "LOWER(d.color) LIKE LOWER(CONCAT('%', :criterio, '%'))",
            nativeQuery = true)
    List<Dependencia> buscarPorCriterio(@Param("criterio") String criterio);

    // 4. Filtro por Estado (Para los botones de Activo/Inactivo)
    List<Dependencia> findByEstado(Boolean estado);

    // 5. SUPER FILTRO AVANZADO (Para los iconos: Color, Tipo, Sucursal, Estado)
    // Se usa DISTINCT para evitar duplicados si una dependencia está en varias sedes
    @Query(value = "SELECT DISTINCT d.* FROM dependencia d " +
            "LEFT JOIN sucursal_dependencia sd ON d.id = sd.IdDependencia " +
            "WHERE (:nombre IS NULL OR d.nombre LIKE %:nombre%) " +
            "AND (:color IS NULL OR LOWER(d.color) LIKE LOWER(CONCAT('%', :color, '%'))) " + // <--- Cambio clave
            "AND (:idTipo IS NULL OR d.idTipoDependencia = :idTipo) " +
            "AND (:idSucursal IS NULL OR sd.IdSucursal = :idSucursal) " +
            "AND (:estado IS NULL OR d.estado = :estado)",
            nativeQuery = true)
    List<Dependencia> filtrarAvanzado(
            @Param("nombre") String nombre,
            @Param("color") String color,
            @Param("idTipo") Integer idTipo,
            @Param("idSucursal") Integer idSucursal,
            @Param("estado") Boolean estado);
}