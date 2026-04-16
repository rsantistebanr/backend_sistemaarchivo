package com.proyecto.sistemaarchivo.repository;

import com.proyecto.sistemaarchivo.model.DocumentoPreCarga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface DocumentoPreCargaRepository extends JpaRepository<DocumentoPreCarga, Integer> {
    // 1. Busca todas las filas de un archivo específico (para ver el detalle)
    List<DocumentoPreCarga> findByNombreArchivo(String nombreArchivo);

    // 2. Busca los documentos rechazados de un usuario específico (para corregir)
    List<DocumentoPreCarga> findByIdUsuarioAndEstadoRevision(Integer idUsuario, Integer estadoRevision);

    // 3. RESUMEN PARA EL REVISOR: Agrupa por archivo y cuenta cuántos documentos trae cada uno
    @Query(value = "SELECT nombrearchivo as nombreArchivo, COUNT(*) as totalDocumentos, " +
            "referencia, MAX(fechaTexto) as fechaCarga, " +
            "MAX(dependenciaTexto) as dependencia, MAX(codigoDocumento) as codigo, " +
            "MAX(tipoDocumentoTexto) as tipo, " +
            "MAX(observacionRevision) as observacion " +
            "FROM documentoprecarga " +
            "WHERE estado_revision = 0 " + // <-- Ojo, aquí usaste guion bajo, revisa si es correcto
            "GROUP BY nombrearchivo, referencia", nativeQuery = true) // <-- Agregado referencia aquí
    List<Object[]> listarCargasPendientes();

    // 4. (Opcional) Para saber si un nombre de archivo ya fue procesado antes
    boolean existsByNombreArchivo(String nombreArchivo);
}
