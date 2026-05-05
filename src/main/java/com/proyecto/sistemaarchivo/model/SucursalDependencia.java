package com.proyecto.sistemaarchivo.model;


import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "sucursal_dependencia")
@Data
public class SucursalDependencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "IdSucursal")
    private Integer idSucursal;

    @Column(name = "IdDependencia")
    private Integer idDependencia;

    private Double estado = 1.0;
}
