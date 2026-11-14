package com.blautech.reclutamiento.candidato.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class EducacionRequest {

    @NotBlank(message = "La institución es obligatoria")
    private String institucion;

    @NotBlank(message = "El título es obligatorio")
    private String titulo;

    private String nivelAcademico;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String estado; // EN_CURSO / FINALIZADO
}
