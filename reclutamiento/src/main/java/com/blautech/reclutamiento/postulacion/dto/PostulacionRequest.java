package com.blautech.reclutamiento.postulacion.dto;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// Lo único que necesita mandar el candidato para postularse: a qué
// vacante. El candidato mismo lo sacamos del token (Authentication),
// igual que hicimos en CandidatoController.
@Getter
@Setter
public class PostulacionRequest {

    @NotNull(message = "Debes indicar a qué vacante te postulas")
    private Integer idVacante;
}
