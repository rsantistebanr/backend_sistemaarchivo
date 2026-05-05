package com.proyecto.sistemaarchivo.repository;

import com.proyecto.sistemaarchivo.model.TipoDependencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoDependenciaRepository extends JpaRepository<TipoDependencia, Integer> {

}
