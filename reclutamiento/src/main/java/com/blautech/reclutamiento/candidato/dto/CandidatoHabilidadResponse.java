package com.blautech.reclutamiento.candidato.dto;

import com.blautech.reclutamiento.candidato.CandidatoHabilidad;
import lombok.Getter;

import java.math.BigDecimal;

// La habilidad de un candidato, aplanada para Angular: idHabilidad y
// nombreHabilidad al mismo nivel que el resto de campos, en vez de un
// objeto "habilidad" anidado (que era lo que Jackson mandaba al
// serializar la entidad CandidatoHabilidad directamente -- por eso el
// frontend veía "#undefined" donde esperaba el id).
@Getter
public class CandidatoHabilidadResponse {
    private final Integer idCandidatoHabilidad;
    private final Integer idHabilidad;
    private final String nombreHabilidad;
    private final String nivel;
    private final BigDecimal aniosExperiencia;

    public CandidatoHabilidadResponse(CandidatoHabilidad ch) {
        this.idCandidatoHabilidad = ch.getIdCandidatoHabilidad();
        this.idHabilidad = ch.getHabilidad().getIdHabilidad();
        this.nombreHabilidad = ch.getHabilidad().getNombre();
        this.nivel = ch.getNivel();
        this.aniosExperiencia = ch.getAniosExperiencia();
    }
}
