package com.blautech.reclutamiento.vacante.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class VacanteRequest {

    @NotBlank(message = "El título es obligatorio")
    private String titulo;

    private Integer idArea;
    private String descripcion;
    private String conocimientosRequeridos;
    private String ubicacion;
    private String modalidad;
    private String experienciaRequerida;
    private String nivelAcademico;
    private LocalDate fechaCierre;

    // Solo se usa al CREAR una vacante ("BORRADOR" o "ACTIVA"). Si viene
    // vacío, se publica como ACTIVA (comportamiento de siempre). Editar
    // el estado de una vacante ya creada sigue haciéndose con /cerrar.
    private String estadoInicial;
}
