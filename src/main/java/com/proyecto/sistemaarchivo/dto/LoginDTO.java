package com.proyecto.sistemaarchivo.dto;

import lombok.Data;

@Data
public class LoginDTO {
    private String usuario; // Cambiado de email a usuario
    private String password;
}
