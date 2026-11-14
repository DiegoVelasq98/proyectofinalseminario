package com.blautech.reclutamiento.postulacion.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

// Lo que manda RRHH para mover una postulación de estado
// (ej. de RECIBIDA a EN_REVISION, o a PREOSELECCIONADO).
@Getter
@Setter
public class CambiarEstadoRequest {

    @NotBlank(message = "El nuevo estado es obligatorio")
    private String estadoNuevo; // RECIBIDA / EN_REVISION / PREOSELECCIONADO / RECHAZADA / CONTRATADO

    private String observacion;
}
