package com.blautech.reclutamiento.vacante.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AsignarResponsableRequest {

    @NotNull(message = "Debes indicar el usuario responsable")
    private Integer idUsuarioResponsable;
}
