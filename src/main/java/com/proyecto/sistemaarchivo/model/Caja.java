package com.proyecto.sistemaarchivo.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "Caja")
@Data
public class Caja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nro_caja")
    private String nroCaja;

    @Column(name = "IdDependencia")
    private Integer idDependencia;

    @Column(name = "IdTipoDocumento")
    private Integer idTipoDocumento;

    @Column(name = "observacion")
    private String observacion;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    @PrePersist
    public void prePersist() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
    }
}

