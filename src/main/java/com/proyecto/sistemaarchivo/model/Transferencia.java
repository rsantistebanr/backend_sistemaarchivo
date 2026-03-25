package com.proyecto.sistemaarchivo.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "tranferencia") // Nombre exacto del DER
@Data
public class Transferencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer Id;

    private Integer IdUsuarioEnvio;
    private Integer IdDependenciaDestino;
    private LocalDateTime fechaTransferencia;
    private String observacion;
    private Integer IdUsuarioRecepcion;
    private Double MetrosLineales;
    private Integer CantidadArchivadores;
}
