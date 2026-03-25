package com.proyecto.sistemaarchivo.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "detalle_tranferencia")
@Data
public class DetalleTransferencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer Id;

    private Integer IdTransferencia;
    private Integer IdArchivador;
    private Integer IdDocumentoExterno;
}