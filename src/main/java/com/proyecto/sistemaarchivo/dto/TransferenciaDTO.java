package com.proyecto.sistemaarchivo.dto;

import java.time.LocalDateTime;

public record TransferenciaDTO(
        Integer idTransferencia,
        Integer idUsuarioEnvio,
        Integer idUsuarioRecepcion,
        Integer idDependenciaDestino,
        String observacion,
        LocalDateTime fechaTransferencia,
        Double metrosLineales,
        Integer cantidadArchivadores,
        Integer estado
) {}