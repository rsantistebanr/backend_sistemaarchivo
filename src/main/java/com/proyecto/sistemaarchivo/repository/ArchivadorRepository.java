package com.proyecto.sistemaarchivo.repository;

import com.proyecto.sistemaarchivo.model.Archivador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ArchivadorRepository extends JpaRepository<Archivador, Integer> {

    // Buscar archivadores por estante (Para ver qué hay en cada balda)
    List<Archivador> findByIdEstante(Integer idEstante);

    // Buscar archivadores por dependencia (Para reportes por oficina)
    List<Archivador> findByIdDependencia(Integer idDependencia);

    // Buscar por año (Útil para las transferencias de archivo que se ven en tu diagrama)
    List<Archivador> findByAño(Integer año);
}
