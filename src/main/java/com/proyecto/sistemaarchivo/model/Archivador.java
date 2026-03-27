package com.proyecto.sistemaarchivo.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "Archivador")
@Data
public class Archivador {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "IdEstante")
    private Integer idEstante;

    @Column(name = "IdDependencia")
    private Integer idDependencia;

    @Column(name = "IdTipoArchivador")
    private Integer idTipoArchivador;

    @Column(name = "año")
    private String año;

    private Integer CantidadDoc;

    private Integer DocumentoInicio;

    private Integer DocumentoFin;

    private Integer es_valioso; // TINYINT en diagrama

    @Column(name = "unidad_medida") // default 0.08
    private Double unidadMedida;

    private Integer numero; // "va a servir en la transferencia"

    private Integer cantidad_folio;

    @Column(name = "num_cuerpo")
    private Integer numCuerpo;

    @Column(name = "valda")
    private String valda;
}
