package com.proyecto.sistemaarchivo.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "tranferencia")
@Data
public class Transferencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer idUsuarioEnvio;

    private Integer idDependenciaDestino;

    private LocalDateTime fechaTransferencia;

    private String observacion;

    private Integer idUsuarioRecepcion;

    private Double metrosLineales;

    private Integer cantidadArchivadores;
}
