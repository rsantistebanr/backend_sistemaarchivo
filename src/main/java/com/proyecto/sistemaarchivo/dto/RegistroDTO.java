package com.proyecto.sistemaarchivo.dto;

import lombok.Data;

@Data
public class RegistroDTO {
    private String nombre;
    private String usuario;   // El login
    private String email;
    private String password;
    private String telefono;
    private Integer idRol;
}
