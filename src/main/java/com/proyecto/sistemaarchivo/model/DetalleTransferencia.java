package com.proyecto.sistemaarchivo.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "detalle_tranferencia")
@Data
public class DetalleTransferencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Integer id;

    @Column(name = "IdTransferencia")
    private Integer idTransferencia;

    @Column(name = "IdArchivador")
    private Integer idArchivador;

    @Column(name = "IdDocumentoExterno")
    private Integer idDocumentoExterno;

    @Column(name = "SerieDocumental")
    private String serieDocumental;

    @Column(name = "FechaDm")
    private String fechaDm;

    @Column(name = "FechaA")
    private String fechaA;

    @Column(name = "CantidadFolios")
    private Integer cantidadFolios;

    @Column(name = "NumeroEstante")
    private Integer numeroEstante;

    @Column(name = "NumeroCuerpo")
    private Integer numeroCuerpo;

    @Column(name = "NivelBalda")
    private String nivelBalda;

    @Column(name = "Observaciones")
    private String observaciones;
}