package com.proyecto.sistemaarchivo.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "Documento")
@Data
public class Documento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer Id;

    private Integer IdTipoDocumento;
    private Integer IdDependencia;
    private Integer IdUsuario;
    private Integer idUsuarioModifica;

    @Column(name = "numeroDocumento_o_codigo_documento")
    private String numeroDocumentoOCodigoDocumento;

    private String asunto;
    private String referencia;
    private LocalDate fechaDocumento;


    private Integer esValioso;

    private String rutaArchivo;
    private LocalDate fechaRegistro;

    private Integer estado;

    private String observacionRevision;
    private Integer Numero_Folio;

    @Column(name = "IdArchivador")
    private Integer idArchivador;

    @Column(name = "nroOrden")
    private Integer nroOrden;
}
