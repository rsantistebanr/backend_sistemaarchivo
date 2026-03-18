package com.proyecto.sistemaarchivo.dto;

import jakarta.persistence.Column;
import lombok.Data;

@Data
public class UsuarioDTO {
    private Integer id;
    private String nombre;
    private String usuario;
    private String email;
    private String telefono;
    private String rol;
    private String dependencia;
    private String sucursal;
    private String estado;
    private Integer bloqueado;
}
