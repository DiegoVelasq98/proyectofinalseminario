package com.blautech.reclutamiento.vacante.dto;

import com.blautech.reclutamiento.vacante.VacanteHabilidad;
import lombok.Getter;

// La habilidad requerida de una vacante, aplanada para Angular -- mismo
// motivo que CandidatoHabilidadResponse: evitar mandar la entidad cruda
// con un objeto "habilidad" anidado en vez de "idHabilidad" plano.
@Getter
public class VacanteHabilidadResponse {
    private final Integer idVacanteHabilidad;
    private final Integer idHabilidad;
    private final String nombreHabilidad;
    private final String nivelRequerido;
    private final Boolean esObligatoria;

    public VacanteHabilidadResponse(VacanteHabilidad vh) {
        this.idVacanteHabilidad = vh.getIdVacanteHabilidad();
        this.idHabilidad = vh.getHabilidad().getIdHabilidad();
        this.nombreHabilidad = vh.getHabilidad().getNombre();
        this.nivelRequerido = vh.getNivelRequerido();
        this.esObligatoria = vh.getEsObligatoria();
    }
}
