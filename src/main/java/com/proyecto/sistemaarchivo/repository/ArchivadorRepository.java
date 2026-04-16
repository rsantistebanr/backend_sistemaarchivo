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
            "SELECT a.id, " +
                    "a.numero AS codigo, " +
                    "a.numero AS numero, " +
                    "a.año, " +
                    "a.IdEstante AS idEstante, " +
                    "a.IdDependencia AS idDependencia, " +
                    "a.IdTipoArchivador AS idTipoArchivador, " +
                    "a.es_valioso AS esValioso, " +
                    "a.num_cuerpo AS numCuerpo, " +
                    "a.valda AS valda, " +
                    "a.unidad_medida AS unidadMedida, " +
                    "a.CantidadDoc AS cantidadDoc, " + // <-- CORREGIDO: Quité la 'r' de Canrtidad
                    "a.cantidad_folio AS cantidadFolio, " +
                    "d.Nombre AS nombre_dependencia, " +
                    "ta.nombre AS nombre_tipo_archivador, " +
                    "e.num_Estante AS num_estante_estante " +
                    "FROM Archivador a " +
                    "INNER JOIN Estante e ON a.IdEstante = e.id " +
                    "INNER JOIN Dependencia d ON a.IdDependencia = d.id " +
                    "LEFT JOIN TipoArchivador ta ON a.IdTipoArchivador = ta.Id " +
                    "WHERE (:idArc IS NULL OR a.id = :idArc)",
            nativeQuery = true)
    List<Map<String, Object>> obtenerDetalleCompleto(@Param("idArc") Integer idArc);

    @Query(value = "SELECT d.id, d.nombre, d.fecha_registro, d.numero_folios " +
            "FROM Documento d " +
            "WHERE d.id_archivador = :idArc", nativeQuery = true)
    List<Map<String, Object>> obtenerDocumentosPorArchivador(@Param("idArc") Integer idArc);

    @Query(value =
            "SELECT a.id, " +
                    "a.numero AS codigo, " +
                    "a.numero AS numero, " +
                    "a.año, " +
                    "a.IdEstante AS idEstante, " +
                    "a.IdDependencia AS idDependencia, " +
                    "a.IdTipoArchivador AS idTipoArchivador, " +
                    "a.es_valioso AS esValioso, " +
                    "a.num_cuerpo AS numCuerpo, " +
                    "a.valda AS valda, " +
                    "a.unidad_medida AS unidadMedida, " +
                    "a.CantidadDoc AS cantidadDoc, " +
                    "a.cantidad_folio AS cantidadFolio, " +
                    "d.Nombre AS nombre_dependencia, " +
                    "ta.nombre AS nombre_tipo_archivador, " +
                    "e.num_Estante AS num_estante_estante, " +
                    "tdDoc.nombre AS tipoDocumento, " +
                    "doc.numeroDocumento_o_codigo_documento AS codigoDocumento " +
                    "FROM Documento doc " +
                    "INNER JOIN Archivador a ON doc.IdArchivador = a.id " +
                    "LEFT JOIN Estante e ON a.IdEstante = e.id " +
                    "LEFT JOIN Dependencia d ON a.IdDependencia = d.id " +
                    "LEFT JOIN TipoArchivador ta ON a.IdTipoArchivador = ta.Id " +
                    "LEFT JOIN TipoDocumento tdDoc ON doc.IdTipoDocumento = tdDoc.Id " +
                    "WHERE UPPER(TRIM(doc.numeroDocumento_o_codigo_documento)) LIKE CONCAT('%', UPPER(TRIM(:terminoDocumento)), '%') " +
                    "OR UPPER(TRIM(tdDoc.nombre)) LIKE CONCAT('%', UPPER(TRIM(:terminoDocumento)), '%') " +
                    "OR UPPER(TRIM(CONCAT(COALESCE(tdDoc.nombre, ''), ' ', COALESCE(doc.numeroDocumento_o_codigo_documento, '')))) " +
                    "LIKE CONCAT('%', UPPER(TRIM(:terminoDocumento)), '%') " +
                    "ORDER BY doc.numeroDocumento_o_codigo_documento ASC",
            nativeQuery = true)
    List<Map<String, Object>> buscarArchivadorPorTipoODocumento(@Param("terminoDocumento") String terminoDocumento);

    //Filtro
    @Query(value =
            "SELECT a.*, d.Nombre AS nombre_dependencia, ta.nombre AS nombre_tipo_archivador " +
                    "FROM Archivador a " +
                    "LEFT JOIN Dependencia d ON a.IdDependencia = d.id " +
                    "LEFT JOIN TipoArchivador ta ON a.IdTipoArchivador = ta.Id " +
                    "WHERE (:idDep IS NULL OR a.IdDependencia = :idDep) " +
                    "AND (:idTipoArc IS NULL OR a.IdTipoArchivador = :idTipoArc) " +
                    "AND (:anio IS NULL OR REPLACE(a.año, ' ', '') LIKE CONCAT('%', REPLACE(:anio, ' ', ''), '%')) " +
                    "AND (:esValioso IS NULL OR a.es_valioso = :esValioso)",
            nativeQuery = true)
    List<Map<String, Object>> filtrarArchivadoresPro(
            @Param("idDep") Integer idDep,
            @Param("idTipoArc") Integer idTipoArc,
            @Param("anio") String anio, // CAMBIO: String
            @Param("esValioso") Integer esValioso);
}
