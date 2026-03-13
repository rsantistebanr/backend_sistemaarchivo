package com.proyecto.sistemaarchivo.controller;

import com.proyecto.sistemaarchivo.dto.UsuarioDTO;
import com.proyecto.sistemaarchivo.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping
    public List<UsuarioDTO> listar() {
        return usuarioRepository.findAll().stream().map(usuario -> {
            UsuarioDTO dto = new UsuarioDTO();
            dto.setId(usuario.getId());
            dto.setNombre(usuario.getNombre());
            dto.setUsuario(usuario.getUsuario()); // Asegúrate que tu DTO tenga este campo
            dto.setEmail(usuario.getEmail());
            dto.setBloqueado(usuario.getBloqueado());
            return dto;
        }).collect(Collectors.toList());
    }
}