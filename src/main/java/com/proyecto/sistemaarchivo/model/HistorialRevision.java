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

    // 1: rechazado, 2: conforme, 3: pendiente (según tu DER)
    private Integer estado;
}
