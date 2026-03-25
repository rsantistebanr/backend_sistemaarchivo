package com.proyecto.sistemaarchivo.repository;

import com.proyecto.sistemaarchivo.model.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Integer> {

    @Query(value = "SELECT d.*, td.nombre as nombre_tipo, dep.Nombre as nombre_dependencia " +
            "FROM Documento d " +
            "LEFT JOIN TipoDocumento td ON d.IdTipoDocumento = td.Id " +
            "LEFT JOIN Dependencia dep ON d.IdDependencia = dep.id " +
            "WHERE (:criterio IS NULL OR d.asunto LIKE CONCAT('%', :criterio, '%') " +
            "OR d.numeroDocumento_o_codigo_documento LIKE CONCAT('%', :criterio, '%')) " +
            "AND (:idTipo IS NULL OR d.IdTipoDocumento = :idTipo) " +
            "AND (:idDep IS NULL OR d.IdDependencia = :idDep) " +
            "AND (:estado IS NULL OR d.estado = :estado)",
            nativeQuery = true)
    List<Map<String, Object>> filtrarDocumentosFull(
            @Param("criterio") String criterio,
            @Param("idTipo") Integer idTipo,
            @Param("idDep") Integer idDep,
            @Param("estado") Integer estado);

    @Query("SELECT COUNT(d) > 0 FROM Documento d WHERE d.numeroDocumentoOCodigoDocumento = :num")
    boolean existeNumeroDoc(@Param("num") String num);

    // NUEVO: Obtener el correlativo máximo por archivador
    @Query("SELECT COALESCE(MAX(d.nroOrden), 0) FROM Documento d WHERE d.idArchivador = :idArc")
    Integer obtenerUltimoNroOrden(@Param("idArc") Integer idArc);



    @Query("SELECT COALESCE(SUM(d.Numero_Folio), 0) FROM Documento d WHERE d.idArchivador = :idArc")
    Integer sumarFoliosPorArchivador(@Param("idArc") Integer idArc);
}