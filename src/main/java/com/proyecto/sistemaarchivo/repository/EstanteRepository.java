package com.proyecto.sistemaarchivo.repository;

import com.proyecto.sistemaarchivo.model.Estante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EstanteRepository extends JpaRepository<Estante, Integer> {
    // Para listar los estantes que pertenecen a una oficina específica
    List<Estante> findByIdDependencia(Integer idDependencia);

    //Filtro
    @Query(value = "SELECT * FROM Estante WHERE " +
            "(:idDep IS NULL OR IdDependencia = :idDep) " +
            "AND (:numEstante IS NULL OR num_Estante = :numEstante) " +
            "AND (:numCuerpo IS NULL OR Num_cuerpo = :numCuerpo) " +
            "AND (:valda IS NULL OR Valda = :valda)", nativeQuery = true)
    List<Estante> filtrarEstantes(
            @Param("idDep") Integer idDep,
            @Param("numEstante") Integer numEstante,
            @Param("numCuerpo") Integer numCuerpo,
            @Param("valda") String valda);
}