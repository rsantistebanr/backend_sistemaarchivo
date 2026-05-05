package com.proyecto.sistemaarchivo.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "sucursal")
@Data
public class Sucursal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String nombre;
    private String direccion;


    private Boolean estado = true;
}
