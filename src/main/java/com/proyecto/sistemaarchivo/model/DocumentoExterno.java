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
    private Integer id;

    private Integer idUsuario;

    @Column(name = "nombreArchivo")
    private String nombreArchivo;

    private String rutaArchivo;
    private LocalDateTime fechaCarga;

    private Boolean estado;
    private String formato;
}
