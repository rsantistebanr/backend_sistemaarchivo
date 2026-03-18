package com.proyecto.sistemaarchivo.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "Estante")
@Data
public class Estante {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "IdDependencia")
    private Integer idDependencia;

    private Integer num_Estante;

    private Integer Num_cuerpo;

    private String Valda; // En el diagrama es CHAR

    private Integer cantidad;
}
