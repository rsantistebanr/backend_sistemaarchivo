package com.proyecto.sistemaarchivo.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegistroDTO {
    private String nombre;
    private String usuario;   // El login
    private String email;

    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).*$",
            message = "La contraseña debe contener mayúsculas, minúsculas, números y símbolos")
    private String password;
    private String telefono;
    private Integer idRol;
}
