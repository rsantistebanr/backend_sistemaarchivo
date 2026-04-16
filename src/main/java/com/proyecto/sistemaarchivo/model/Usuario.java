package com.proyecto.sistemaarchivo.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
@Data
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String nombre;
    private String usuario;
    private String email;
    private String password;
    private String telefono;

    // Campos de ID (para inserts/updates)
    @Column(name = "IdRol")
    private Integer idRol;

    @Column(name = "IdDependencia")
    private Integer idDependencia;

    @Column(name = "IdSucursal")
    private Integer idSucursal;

    // OBJETOS DE RELACIÓN (para consultas/SELECTS)
    // El 'name' debe ser el nombre físico de la columna en la BD
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IdRol", referencedColumnName = "id", insertable = false, updatable = false)
    private Rol rolObj;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IdDependencia", referencedColumnName = "id", insertable = false, updatable = false)
    private Dependencia dependenciaObj;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IdSucursal", referencedColumnName = "id", insertable = false, updatable = false)
    private Sucursal sucursalObj;

    private Integer bloqueado = 0;
    private Integer estado = 1;

    @Column(name = "fecha_ingreso")
    private LocalDateTime fechaIngreso = LocalDateTime.now();

    // Dentro de Usuario.java
    @Column(name = "intentos_fallidos")
    private Integer intentosFallidos = 0; // Inicia en 0

    @Column(name = "cambio_contrasena")
    private Integer cambioContrasena = 0;
}