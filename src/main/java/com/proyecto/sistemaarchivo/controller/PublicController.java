package com.proyecto.sistemaarchivo.controller;


import com.proyecto.sistemaarchivo.dto.UsuarioDTO;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/publica")
@CrossOrigin(origins = "*")
public class PublicController {
    @GetMapping
    public String listar() {
        return "hay conexion";
    }
}
