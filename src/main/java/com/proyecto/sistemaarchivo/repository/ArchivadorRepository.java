package com.proyecto.sistemaarchivo.repository;

import com.proyecto.sistemaarchivo.model.Archivador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;

@Repository
public interface ArchivadorRepository extends JpaRepository<Archivador, Integer> {

    // Buscar archivadores por estante (Para ver qué hay en cada balda)
    List<Archivador> findByIdEstante(Integer idEstante);

    // Buscar archivadores por dependencia (Para reportes por oficina)
    List<Archivador> findByIdDependencia(Integer idDependencia);

    // Buscar por año (Útil para las transferencias de archivo que se ven en tu diagrama)
    List<Archivador> findByAño(String año);

    // Esta consulta es la que hace la "magia" de unir el Archivador con su ubicación real
    @Query(value =
            "SELECT a.*, d.Nombre AS nombre_dependencia, ta.nombre AS nombre_tipo_archivador, " +
                    "e.num_Estante AS num_estante_estante " +
                    "FROM Archivador a " +
                    "INNER JOIN Estante e ON a.IdEstante = e.id " +
                    "INNER JOIN Dependencia d ON a.IdDependencia = d.id " +
                    "LEFT JOIN TipoArchivador ta ON a.IdTipoArchivador = ta.Id " +
                    "WHERE (:idArc IS NULL OR a.id = :idArc)",
            nativeQuery = true)
    List<Map<String, Object>> obtenerDetalleCompleto(@Param("idArc") Integer idArc);

    //Filtro
    @Query(value =
            "SELECT a.*, d.Nombre AS nombre_dependencia, ta.nombre AS nombre_tipo_archivador " +
                    "FROM Archivador a " +
                    "LEFT JOIN Dependencia d ON a.IdDependencia = d.id " +
                    "LEFT JOIN TipoArchivador ta ON a.IdTipoArchivador = ta.Id " +
                    "WHERE (:idDep IS NULL OR a.IdDependencia = :idDep) " +
                    "AND (:idTipoArc IS NULL OR a.IdTipoArchivador = :idTipoArc) " +
                    "AND (:anio IS NULL OR a.año LIKE %:anio%) " + // CAMBIO: Usamos LIKE por si es rango
                    "AND (:esValioso IS NULL OR a.es_valioso = :esValioso)",
            nativeQuery = true)
    List<Map<String, Object>> filtrarArchivadoresPro(
            @Param("idDep") Integer idDep,
            @Param("idTipoArc") Integer idTipoArc,
            @Param("anio") String anio, // CAMBIO: String
            @Param("esValioso") Integer esValioso);
}
