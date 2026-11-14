package com.blautech.reclutamiento.analisis.ia;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// La respuesta completa de Gemini al evaluar la coincidencia de
// habilidades: una lista con una evaluación por cada habilidad requerida
// por la vacante (ver GeminiEvaluacionCriterio).
@Getter
@Setter
@NoArgsConstructor
public class GeminiEvaluacionHabilidadesResponse {

    private List<GeminiEvaluacionCriterio> evaluaciones;
}
