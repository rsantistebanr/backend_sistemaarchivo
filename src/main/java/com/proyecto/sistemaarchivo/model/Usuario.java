package com.proyecto.sistemaarchivo.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "usuario")
@Data
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String nombre;
    private String usuario; // El campo 'usuario' que mencionaste
    private String email;
    private String password;
    private String telefono;

    @Column(name = "IdRol")
    private Integer idRol;

    @Column(name = "IdDependencia")
    private Integer idDependencia;

    @Column(name = "IdSucursal")
    private Integer idSucursal;

    private Boolean bloqueado = false;
    private Boolean estado = true;

    @Column(name = "fecha_ingreso")
    private java.time.LocalDateTime fechaIngreso = java.time.LocalDateTime.now();
}
