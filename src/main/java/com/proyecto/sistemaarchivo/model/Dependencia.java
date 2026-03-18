package com.proyecto.sistemaarchivo.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "dependencia")
@Data
public class Dependencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "IdTipoDependencia")
    private Integer idTipoDependencia;

    private String nombre;

    @Column(name = "codigoNumerico", length = 15, unique = true)
    private String codigoNumerico; // De Double a String

    @Column(name = "Fecha_Creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "Fecha_Termino")
    private LocalDateTime fechaTermino;

    private Boolean estado;

    // Campos trasladados aquí
    @Column(name = "Tipo_color")
    private String tipoColor; // entero o mixto

    private String color;
}