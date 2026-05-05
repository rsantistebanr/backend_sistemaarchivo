package com.proyecto.sistemaarchivo.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "historial_de_revisión")
@Data
public class HistorialRevision {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer IdUsuarioRevision;
    private Integer idDetalleEnvio; // FK hacia DetalleTransferencia
    private LocalDateTime Fecha_Revision;
    private LocalDateTime Fecha_SubSancion;
    private Integer estado;
}
