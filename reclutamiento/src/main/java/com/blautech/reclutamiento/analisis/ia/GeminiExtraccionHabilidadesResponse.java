package com.blautech.reclutamiento.analisis.ia;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// La respuesta de Gemini al extraer, de forma abierta (no limitada al
// catalogo), las habilidades que un candidato menciona en el texto de
// su curriculum. A diferencia de GeminiEvaluacionHabilidadesResponse,
// aqui no se compara contra nada: es una lectura libre del documento.
@Getter
@Setter
@NoArgsConstructor
public class GeminiExtraccionHabilidadesResponse {

    private List<String> habilidades;
}
