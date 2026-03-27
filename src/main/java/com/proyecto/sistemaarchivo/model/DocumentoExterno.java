package com.proyecto.sistemaarchivo.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "documentoexterno")
@Data
public class DocumentoExterno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer Id;

    @Column(name = "IdUsuario")
    private Integer IdUsuario;

    @Column(name = "IdArchivador")
    private Integer idArchivador;

    // Nombre/ruta se almacenan como texto para reflejar el archivo importado.
    @Column(name = "nombreArchivo")
    private String nombreArchivo;

    @Column(name = "rutaArchivo")
    private String rutaArchivo;

    @Column(name = "fechaCarga")
    private LocalDateTime fechaCarga;

    @Column(name = "estado")
    private Boolean estado;

    @Column(name = "formato")
    private String formato;
}