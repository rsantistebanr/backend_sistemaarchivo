package com.proyecto.sistemaarchivo.repository;

import com.proyecto.sistemaarchivo.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface  RolRepository extends JpaRepository<Rol, Integer> {

    // Método útil por si necesitas buscar un rol por su nombre (ej: "ADMINISTRADOR")
    Optional<Rol> findByNombre(String nombre);
}
