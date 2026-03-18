package com.proyecto.sistemaarchivo.repository;

import com.proyecto.sistemaarchivo.model.Estante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EstanteRepository extends JpaRepository<Estante, Integer> {
    // Para listar los estantes que pertenecen a una oficina específica
    List<Estante> findByIdDependencia(Integer idDependencia);
}