package com.proyecto.sistemaarchivo.repository;

import com.proyecto.sistemaarchivo.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    // Método clave para el login por nombre de usuario
    Optional<Usuario> findByUsuario(String usuario);
}
