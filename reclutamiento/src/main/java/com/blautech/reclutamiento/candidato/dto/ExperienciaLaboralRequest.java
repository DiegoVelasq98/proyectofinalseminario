package com.blautech.reclutamiento.candidato.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ExperienciaLaboralRequest {

    @NotBlank(message = "La empresa es obligatoria")
    private String empresa;

    @NotBlank(message = "El puesto es obligatorio")
    private String puesto;

    private String descripcion;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDate fechaInicio;

    private LocalDate fechaFin;

    private boolean actualmenteTrabaja;
}
