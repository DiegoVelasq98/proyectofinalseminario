package com.blautech.reclutamiento.candidato.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CandidatoHabilidadRequest {

    @NotNull(message = "Debes indicar el id de la habilidad")
    private Integer idHabilidad;

    private String nivel; // BASICO / INTERMEDIO / AVANZADO
    private BigDecimal aniosExperiencia;
}
