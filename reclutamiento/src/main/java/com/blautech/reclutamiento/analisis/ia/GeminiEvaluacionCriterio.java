package com.blautech.reclutamiento.analisis.ia;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// Un criterio evaluado por la IA: la coincidencia entre UNA habilidad
// requerida por la vacante (identificada por "indice", su posición en la
// lista que le mandamos a Gemini) y lo que tiene el candidato.
//
// @NoArgsConstructor + @Setter porque Jackson necesita poder construir
// este objeto a partir del JSON que devuelve Gemini (deserialización).
@Getter
@Setter
@NoArgsConstructor
public class GeminiEvaluacionCriterio {

    private int indice;
    private BigDecimal porcentajeCoincidencia;
    private String valorCandidato;
    private String observacion;
}
