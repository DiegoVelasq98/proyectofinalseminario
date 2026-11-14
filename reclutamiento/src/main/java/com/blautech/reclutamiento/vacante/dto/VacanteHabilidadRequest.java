package com.blautech.reclutamiento.vacante.dto;


import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VacanteHabilidadRequest {

    @NotNull (message = "Debe indicar el id de la habilidad")
    private Integer idHabilidad;

    private String nivelRequerido;
    private boolean esObligatoria = true;

}

