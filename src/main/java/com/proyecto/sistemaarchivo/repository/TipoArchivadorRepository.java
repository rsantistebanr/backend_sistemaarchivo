package com.proyecto.sistemaarchivo.repository;

import com.proyecto.sistemaarchivo.model.TipoArchivador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoArchivadorRepository extends JpaRepository<TipoArchivador, Integer> {

}
