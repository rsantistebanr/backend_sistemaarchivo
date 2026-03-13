package com.proyecto.sistemaarchivo.service;

import com.proyecto.sistemaarchivo.model.Usuario;
import com.proyecto.sistemaarchivo.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsuario(username)
                .orElseThrow(() -> new UsernameNotFoundException("El usuario " + username + " no existe."));

        // Agregamos ROLE_ al nombre para que coincida con SecurityConfig
        String nombreRol = "ROLE_" + mapearRol(usuario.getIdRol());

        return new User(
                usuario.getUsuario(),
                usuario.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(nombreRol))
        );
    }

    private String mapearRol(Integer idRol) {
        if (idRol == null) return "USUARIO";
        return switch (idRol) {
            case 1 -> "ADMINISTRADOR";
            case 2 -> "USUARIOA";
            case 3 -> "USUARIO";
            default -> "USUARIO";
        };
    }
}