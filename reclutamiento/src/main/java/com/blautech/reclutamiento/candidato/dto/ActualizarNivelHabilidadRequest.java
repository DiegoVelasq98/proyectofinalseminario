package com.blautech.reclutamiento.candidato.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

// Lo que el propio candidato manda para fijar/cambiar el nivel de una
// habilidad que YA tiene en su perfil -- típicamente una que quedó sin
// nivel porque la detectó el procesamiento del currículum (ver
// CandidatoService.registrarYProcesarCurriculum, que solo detecta la
// habilidad en el texto, nunca "adivina" el nivel). Antes de este
// endpoint, la única forma de ponerle nivel a esa habilidad era
// borrarla y volverla a agregar manualmente.
@Getter
@Setter
public class ActualizarNivelHabilidadRequest {

    @NotBlank(message = "Debes indicar el nivel")
    private String nivel; // BASICO / INTERMEDIO / AVANZADO
}
