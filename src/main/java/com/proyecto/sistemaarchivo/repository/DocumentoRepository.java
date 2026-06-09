package com.proyecto.sistemaarchivo.repository;

import com.proyecto.sistemaarchivo.model.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Integer> {

    @Query(value = "SELECT d.*, td.nombre as nombre_tipo, dep.Nombre as nombre_dependencia " +
            "FROM Documento d " +
            "LEFT JOIN tipodocumento td ON d.IdTipoDocumento = td.Id " +
            "LEFT JOIN dependencia dep ON d.IdDependencia = dep.id " +
            "WHERE (:criterio IS NULL " +
            "   OR d.numeroDocumento_o_codigo_documento = :criterio " +
            "   OR d.asunto LIKE CONCAT('%', :criterio, '%')) " +
            "AND (:fecha IS NULL OR d.fechaDocumento = :fecha) " +
            "AND (:idTipo IS NULL OR d.IdTipoDocumento = :idTipo) " +
            "AND (:idDep IS NULL OR d.IdDependencia = :idDep) " +
            "AND (:estado IS NULL OR d.estado = :estado)",
            nativeQuery = true)
    List<Map<String, Object>> filtrarDocumentosFull(
            @Param("criterio") String criterio,
            @Param("fecha") java.time.LocalDate fecha,
            @Param("idTipo") Integer idTipo,
            @Param("idDep") Integer idDep,
            @Param("estado") Integer estado);

    @Query("SELECT COUNT(d) > 0 FROM Documento d WHERE d.numeroDocumentoOCodigoDocumento = :num")
    boolean existeNumeroDoc(@Param("num") String num);

    // NUEVO: Obtener el correlativo máximo por archivador
    @Query("SELECT COALESCE(MAX(d.nroOrden), 0) FROM Documento d WHERE d.idArchivador = :idArc")
    Integer obtenerUltimoNroOrden(@Param("idArc") Integer idArc);

    // Busca el ID basándose en el nombre del tipo (ej: 'INFORME')
    @Query(value = "SELECT Id FROM tipodocumento WHERE UPPER(nombre) = :nombre LIMIT 1", nativeQuery = true)
    Integer buscarIdTipoPorNombre(@Param("nombre") String nombre);

    // Busca el ID basándose en el nombre de la dependencia
    @Query(value = "SELECT id FROM dependencia WHERE UPPER(Nombre) = :nombre LIMIT 1", nativeQuery = true)
    Integer buscarIdDependenciaPorNombre(@Param("nombre") String nombre);

    @Query("SELECT COALESCE(SUM(d.Numero_Folio), 0) FROM Documento d WHERE d.idArchivador = :idArc")
    Integer sumarFoliosPorArchivador(@Param("idArc") Integer idArc);

    //VISUALIZAR

    @Query(value = "SELECT " +
            "d.Id, " +
            "d.numeroDocumento_o_codigo_documento AS codigoDocumento, " +
            "d.asunto, " +
            "d.referencia, " +
            "d.fechaDocumento, " +
            "d.Numero_Folio, " +
            "d.nroOrden, " +
            "d.observacionRevision as observacion, " +
            "td.nombre AS tipoDocumento, " +
            "dep.Nombre AS dependencia " +
            "FROM Documento d " +
            "LEFT JOIN tipodocumento td ON d.IdTipoDocumento = td.Id " +
            "LEFT JOIN dependencia dep ON d.IdDependencia = dep.id " +
            "WHERE d.IdArchivador = :idArc " +
            "ORDER BY d.nroOrden ASC",
            nativeQuery = true)
    List<Map<String, Object>> listarDocumentosPorArchivadorFull(@Param("idArc") Integer idArc);
}