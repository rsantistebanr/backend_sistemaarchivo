package com.proyecto.sistemaarchivo.dto;

import lombok.Data;

@Data
public class UsuarioDTO {
    private Integer id;
    private String nombre;
    private String usuario;
    private String email;
    private Boolean bloqueado;
}
