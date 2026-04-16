package com.proyecto.sistemaarchivo.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "documentoprecarga")
@Data
public class DocumentoPreCarga {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "IdArchivador")
    private Integer idArchivador;

    private Integer idUsuario;

    private String asunto;
    private String codigoDocumento;
    private String tipoDocumentoTexto;
    private String dependenciaTexto;
    private String fechaTexto;
    private Integer folios;
    private String referencia;
    private String nombreArchivo;
    private String observacionRevision;

    @Column(name = "observacion_rechazo")
    private String observacionRechazo;

    @Column(name = "estado_revision") // Esto le dice a Java que en SQL se llama con guion
    private Integer estadoRevision; // 0 o 2
}
