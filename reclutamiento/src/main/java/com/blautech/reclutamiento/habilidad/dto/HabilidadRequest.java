package com.blautech.reclutamiento.habilidad.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HabilidadRequest {

    @NotBlank(message = "El nombre de la habilidad es obligatorio")
    private String nombre;

    private String descripcion;
}
//rFFzv97D4A