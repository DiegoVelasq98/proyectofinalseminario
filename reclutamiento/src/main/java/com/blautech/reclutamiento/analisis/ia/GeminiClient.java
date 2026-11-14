package com.blautech.reclutamiento.analisis.ia;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;

// ============================================================================
// Cliente de la API de Gemini (Google AI Studio), usado por AnalisisService
// para que la comparación de habilidades del candidato vs. la vacante la
// evalúe un modelo de IA en vez de (o, si falla, además de) el algoritmo de
// reglas en Java puro.
//
// Por qué un cliente aparte y no meter todo esto directo en AnalisisService:
// para que el día de mañana se pueda reemplazar o ampliar (ej. análisis del
// perfil completo -- experiencia, educación, CV -- y no solo habilidades)
// sin tocar la lógica de negocio que ya está sustentada en la tesis.
// AnalisisService solo le manda texto plano y recibe de vuelta un objeto ya
// estructurado; no sabe nada de cómo se construye el request HTTP ni de la
// forma exacta de la respuesta de Google.
//
// NOTA TÉCNICA: usa Jackson 3 (tools.jackson.*), que es el que trae por
// default Spring Boot 4.x -- NO es el Jackson 2 "clásico" (com.fasterxml.*)
// que la mayoría de tutoriales en internet todavía muestran. JsonMapper es
// el equivalente aquí al viejo ObjectMapper (Spring Boot ya te da una
// instancia lista para inyectar).
//
// Endpoint usado: la API de "Interactions" de Gemini, en modo de salida
// estructurada (response_format con JSON Schema), para que la respuesta
// venga garantizada en el formato que esperamos y no haya que parsear
// texto libre con reglas frágiles.
// ============================================================================
@Component
public class GeminiClient {

    private static final String ESQUEMA_RESPUESTA_JSON = """
            {
              "type": "object",
              "properties": {
                "evaluaciones": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "indice": { "type": "integer" },
                      "porcentajeCoincidencia": { "type": "number" },
                      "valorCandidato": { "type": "string" },
                      "observacion": { "type": "string" }
                    },
                    "required": ["indice", "porcentajeCoincidencia", "valorCandidato", "observacion"]
                  }
                }
              },
              "required": ["evaluaciones"]
            }
            """;

    private static final String ESQUEMA_EXTRACCION_JSON = """
            {
              "type": "object",
              "properties": {
                "habilidades": {
                  "type": "array",
                  "items": { "type": "string" }
                }
              },
              "required": ["habilidades"]
            }
            """;

    private final RestClient restClient;
    private final JsonMapper jsonMapper;
    private final String apiKey;
    private final String modelo;
    private final boolean habilitado;
    private final JsonNode esquemaRespuesta;
    private final JsonNode esquemaExtraccion;

    public GeminiClient(JsonMapper jsonMapper,
                         @Value("${gemini.api.key:}") String apiKey,
                         @Value("${gemini.api.model:gemini-3.5-flash-lite}") String modelo,
                         @Value("${gemini.api.habilitado:true}") boolean habilitado) {
        this.jsonMapper = jsonMapper;
        this.apiKey = apiKey;
        this.modelo = modelo;
        this.habilitado = habilitado;
        this.esquemaRespuesta = construirEsquemaRespuesta();
        this.esquemaExtraccion = construirEsquemaExtraccion();

        SimpleClientHttpRequestFactory fabricaRequest = new SimpleClientHttpRequestFactory();
        fabricaRequest.setConnectTimeout(5_000);
        fabricaRequest.setReadTimeout(15_000);

        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .requestFactory(fabricaRequest)
                .build();
    }

    // false si no hay API key configurada o si se deshabilitó por
    // application.properties (gemini.api.habilitado=false) -- útil para
    // apagar la IA rápido (ej. en una demo sin internet) sin borrar la key.
    public boolean estaDisponible() {
        return habilitado && apiKey != null && !apiKey.isBlank();
    }

    public GeminiEvaluacionHabilidadesResponse evaluarCoincidenciaHabilidades(List<String> habilidadesVacante,
                                                                               List<String> habilidadesCandidato) {
        if (!estaDisponible()) {
            throw new IllegalStateException(
                    "La integración con Gemini no está habilitada o falta configurar gemini.api.key");
        }

        String prompt = construirPrompt(habilidadesVacante, habilidadesCandidato);

        ObjectNode cuerpo = jsonMapper.createObjectNode();
        cuerpo.put("model", modelo);
        cuerpo.put("input", prompt);

        ObjectNode responseFormat = cuerpo.putObject("response_format");
        responseFormat.put("type", "text");
        responseFormat.put("mime_type", "application/json");
        responseFormat.set("schema", esquemaRespuesta);

        JsonNode respuestaCruda;
        try {
            respuestaCruda = restClient.post()
                    .uri("/interactions")
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo contactar a la API de Gemini: " + e.getMessage(), e);
        }

        return extraerRespuestaEstructurada(respuestaCruda);
    }

    // A diferencia de evaluarCoincidenciaHabilidades (que solo compara
    // nombres de habilidades ya conocidas), este metodo SI le manda a
    // Gemini el texto completo del curriculum para que identifique
    // cualquier habilidad mencionada, exista o no todavia en el catalogo
    // de la plataforma. CandidatoService decide que hacer con cada
    // nombre devuelto (si no existe en el catalogo, la crea).
    public List<String> extraerHabilidadesDeTexto(String textoCv) {
        if (!estaDisponible()) {
            throw new IllegalStateException(
                    "La integracion con Gemini no esta habilitada o falta configurar gemini.api.key");
        }
        if (textoCv == null || textoCv.isBlank()) {
            return List.of();
        }

        String prompt = construirPromptExtraccion(textoCv);

        ObjectNode cuerpo = jsonMapper.createObjectNode();
        cuerpo.put("model", modelo);
        cuerpo.put("input", prompt);

        ObjectNode responseFormat = cuerpo.putObject("response_format");
        responseFormat.put("type", "text");
        responseFormat.put("mime_type", "application/json");
        responseFormat.set("schema", esquemaExtraccion);

        JsonNode respuestaCruda;
        try {
            respuestaCruda = restClient.post()
                    .uri("/interactions")
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo contactar a la API de Gemini: " + e.getMessage(), e);
        }

        return extraerListaHabilidades(respuestaCruda);
    }

    private String construirPromptExtraccion(String textoCv) {
        // Limite generoso para no mandar curriculums enormes completos --
        // las habilidades casi siempre aparecen en las primeras secciones
        // (perfil, habilidades tecnicas, experiencia reciente).
        String textoAcotado = textoCv.length() > 12_000 ? textoCv.substring(0, 12_000) : textoCv;

        return """
                Eres un asistente que revisa currículums para un sistema de reclutamiento.
                Lee el siguiente texto, extraído de un currículum real, e identifica TODAS las
                habilidades, tecnologías, lenguajes de programación, frameworks, herramientas,
                metodologías o certificaciones técnicas que el candidato menciona explícitamente.

                Reglas:
                - Solo incluye habilidades que de verdad aparezcan mencionadas en el texto, nunca
                  inventes ninguna.
                - Devuelve cada habilidad con un nombre corto y estandarizado (ej. "React" en vez
                  de "React.js, biblioteca para interfaces").
                - No repitas la misma habilidad con nombres distintos (ej. si aparece "JavaScript"
                  y "JS", devuélvela una sola vez como "JavaScript").
                - Incluye tanto habilidades técnicas como habilidades blandas relevantes si se
                  mencionan explícitamente (ej. "Trabajo en equipo", "Liderazgo"), pero no las
                  deduzcas de la experiencia si no están nombradas como tales.

                Texto del currículum:
                %s

                Responde solo con el JSON solicitado.
                """.formatted(textoAcotado);
    }

    private String construirPrompt(List<String> habilidadesVacante, List<String> habilidadesCandidato) {
        String listaVacante = habilidadesVacante.isEmpty()
                ? "(ninguna)"
                : String.join("\n", habilidadesVacante);
        String listaCandidato = habilidadesCandidato.isEmpty()
                ? "(el candidato no tiene ninguna habilidad registrada en su perfil)"
                : String.join("\n", habilidadesCandidato);

        return """
                Eres un evaluador técnico de un sistema de reclutamiento. Tu única tarea es comparar
                las habilidades que pide una vacante contra las habilidades registradas en el perfil
                de un candidato, y para cada habilidad requerida decidir qué tanto la cumple.

                Reglas para evaluar cada habilidad requerida:
                - Si el candidato tiene exactamente esa habilidad y su nivel es igual o superior al
                  nivel requerido (BASICO=1, INTERMEDIO=2, AVANZADO=3), el porcentaje es 100.
                - Si tiene la habilidad pero sin nivel especificado, usa 50 como base.
                - Si tiene la habilidad pero en un nivel inferior al requerido, usa un porcentaje
                  proporcional a qué tan cerca está (ej. nivel 1 de 3 requerido ≈ 33).
                - Si NO tiene esa habilidad exacta pero tiene una habilidad equivalente, similar o
                  estrechamente relacionada (ej. "React.js" cuenta como "React"; "Spring Boot" implica
                  buen dominio de "Java"), reduce el porcentaje proporcionalmente a qué tan relacionada
                  está esa habilidad, en vez de darle 0 automáticamente.
                - Si no tiene ninguna habilidad relacionada, el porcentaje es 0.
                - Nunca inventes habilidades que el candidato no tenga en la lista.

                Habilidades requeridas por la vacante (numeradas desde 0; responde EXACTAMENTE una
                evaluación por cada índice, sin omitir ninguna):
                %s

                Habilidades registradas en el perfil del candidato:
                %s

                Responde solo con el JSON solicitado.
                """.formatted(listaVacante, listaCandidato);
    }

    private JsonNode construirEsquemaRespuesta() {
        try {
            return jsonMapper.readTree(ESQUEMA_RESPUESTA_JSON);
        } catch (Exception e) {
            // Esto solo pasaría si el JSON de arriba estuviera mal escrito --
            // es un literal fijo, así que si truena es un bug de programación,
            // no algo que pueda pasar en producción.
            throw new IllegalStateException("El esquema de respuesta de Gemini está mal formado", e);
        }
    }

    private JsonNode construirEsquemaExtraccion() {
        try {
            return jsonMapper.readTree(ESQUEMA_EXTRACCION_JSON);
        } catch (Exception e) {
            throw new IllegalStateException("El esquema de extracción de habilidades está mal formado", e);
        }
    }

    private GeminiEvaluacionHabilidadesResponse extraerRespuestaEstructurada(JsonNode respuestaCruda) {
        if (respuestaCruda == null) {
            throw new IllegalStateException("Gemini devolvió una respuesta vacía");
        }

        String status = respuestaCruda.path("status").asString("");
        if (!"completed".equals(status)) {
            String mensajeError = (respuestaCruda.path("errors").isArray() && !respuestaCruda.path("errors").isEmpty())
                    ? respuestaCruda.path("errors").get(0).path("message").asString("error desconocido")
                    : "estado recibido: " + status;
            throw new IllegalStateException("Gemini no completó la evaluación (" + mensajeError + ")");
        }

        for (JsonNode paso : respuestaCruda.path("steps")) {
            if (!"model_output".equals(paso.path("type").asString(""))) {
                continue;
            }
            for (JsonNode contenido : paso.path("content")) {
                if ("text".equals(contenido.path("type").asString(""))) {
                    String textoJson = contenido.path("text").asString(null);
                    if (textoJson == null || textoJson.isBlank()) {
                        continue;
                    }
                    try {
                        return jsonMapper.readValue(textoJson, GeminiEvaluacionHabilidadesResponse.class);
                    } catch (Exception e) {
                        throw new IllegalStateException(
                                "La respuesta de Gemini no vino en el formato JSON esperado: " + e.getMessage(), e);
                    }
                }
            }
        }

        throw new IllegalStateException("La respuesta de Gemini no contenía ningún texto generado para analizar");
    }

    private List<String> extraerListaHabilidades(JsonNode respuestaCruda) {
        if (respuestaCruda == null) {
            throw new IllegalStateException("Gemini devolvió una respuesta vacía");
        }

        String status = respuestaCruda.path("status").asString("");
        if (!"completed".equals(status)) {
            String mensajeError = (respuestaCruda.path("errors").isArray() && !respuestaCruda.path("errors").isEmpty())
                    ? respuestaCruda.path("errors").get(0).path("message").asString("error desconocido")
                    : "estado recibido: " + status;
            throw new IllegalStateException("Gemini no completó la extracción (" + mensajeError + ")");
        }

        for (JsonNode paso : respuestaCruda.path("steps")) {
            if (!"model_output".equals(paso.path("type").asString(""))) {
                continue;
            }
            for (JsonNode contenido : paso.path("content")) {
                if ("text".equals(contenido.path("type").asString(""))) {
                    String textoJson = contenido.path("text").asString(null);
                    if (textoJson == null || textoJson.isBlank()) {
                        continue;
                    }
                    try {
                        GeminiExtraccionHabilidadesResponse respuesta =
                                jsonMapper.readValue(textoJson, GeminiExtraccionHabilidadesResponse.class);
                        return respuesta.getHabilidades() != null ? respuesta.getHabilidades() : List.of();
                    } catch (Exception e) {
                        throw new IllegalStateException(
                                "La respuesta de Gemini no vino en el formato JSON esperado: " + e.getMessage(), e);
                    }
                }
            }
        }

        throw new IllegalStateException("La respuesta de Gemini no contenía ningún texto generado para analizar");
    }
}
