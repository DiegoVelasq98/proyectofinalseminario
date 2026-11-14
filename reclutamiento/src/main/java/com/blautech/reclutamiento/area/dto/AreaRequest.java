package com.blautech.reclutamiento.area.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AreaRequest {

    @NotBlank(message = "El nombre del área es obligatorio")
    private String nombre;
}
