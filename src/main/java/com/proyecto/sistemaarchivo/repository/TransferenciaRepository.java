package com.proyecto.sistemaarchivo.repository;

import com.proyecto.sistemaarchivo.model.Transferencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransferenciaRepository extends JpaRepository<Transferencia, Integer> {
}
