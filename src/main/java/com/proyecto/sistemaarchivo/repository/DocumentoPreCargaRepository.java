package com.proyecto.sistemaarchivo.repository;

import com.proyecto.sistemaarchivo.model.DocumentoPreCarga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface DocumentoPreCargaRepository extends JpaRepository<DocumentoPreCarga, Integer> {
    List<DocumentoPreCarga> findByNombreArchivo(String nombreArchivo);
    List<DocumentoPreCarga> findByNombreArchivoAndIdUsuario(String nombreArchivo, Integer idUsuario);
    List<DocumentoPreCarga> findByIdUsuarioAndEstadoRevision(Integer idUsuario, Integer estadoRevision);

    // Agrupa por archivo y cuenta cuántos documentos trae cada uno, evitando duplicados por referencia
    @Query(value = "SELECT nombrearchivo as nombreArchivo, COUNT(*) as totalDocumentos, " +
            "MAX(referencia) as referencia, MAX(fechaTexto) as fechaCarga, " +
            "MAX(dependenciaTexto) as dependencia, MAX(codigoDocumento) as codigo, " +
            "MAX(tipoDocumentoTexto) as tipo, " +
            "MAX(observacionRevision) as observacion, " +
            "MAX(idArchivador) as idArchivador " +
            "FROM documentoprecarga " +
            "WHERE estado_revision = 0 " +
            "GROUP BY nombrearchivo", nativeQuery = true)
    List<Object[]> listarCargasPendientes();

    @Query(value = "SELECT nombrearchivo as nombreArchivo, COUNT(*) as totalDocumentos, " +
            "MAX(referencia) as referencia, MAX(fechaTexto) as fechaCarga, " +
            "MAX(dependenciaTexto) as dependencia, MAX(codigoDocumento) as codigo, " +
            "MAX(tipoDocumentoTexto) as tipo, " +
            "MAX(observacionRevision) as observacion, " +
            "MAX(observacion_rechazo) as observacionRechazo, " +
            "estado_revision as estadoRevision, " +
            "MAX(idArchivador) as idArchivador " +
            "FROM documentoprecarga " +
            "WHERE estado_revision IN (1, 2) " +
            "GROUP BY nombrearchivo, estado_revision",
            nativeQuery = true)
    List<Object[]> listarCargasRevisadas();

    boolean existsByNombreArchivo(String nombreArchivo);

    //Obtener cargas del usuario (pendientes + rechazadas) ordenadas por estado y fecha
    @Query(value = "SELECT * FROM documentoprecarga " +
            "WHERE IdUsuario = :idUsuario AND (estado_revision IN (0, 1, 2)) " +
            "ORDER BY estado_revision ASC, id DESC",
            nativeQuery = true)
    List<DocumentoPreCarga> obtenerMisCargas(Integer idUsuario);

    List<DocumentoPreCarga> findByNombreArchivoAndEstadoRevision(String nombreArchivo, Integer estadoRevision);
    List<DocumentoPreCarga> findByNombreArchivoAndIdUsuarioAndEstadoRevision(String nombreArchivo, Integer idUsuario, Integer estadoRevision);
    void deleteByNombreArchivoAndIdUsuarioAndEstadoRevision(String nombreArchivo, Integer idUsuario, Integer estadoRevision);
}
