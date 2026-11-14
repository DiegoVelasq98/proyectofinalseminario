package com.blautech.reclutamiento.analisis;


import com.blautech.reclutamiento.analisis.dto.AnalisisResponse;
import com.blautech.reclutamiento.analisis.dto.HabilidadExtra;
import com.blautech.reclutamiento.analisis.ia.GeminiClient;
import com.blautech.reclutamiento.analisis.ia.GeminiEvaluacionCriterio;
import com.blautech.reclutamiento.analisis.ia.GeminiEvaluacionHabilidadesResponse;
import com.blautech.reclutamiento.candidato.CandidatoHabilidad;
import com.blautech.reclutamiento.candidato.CandidatoHabilidadRepository;
import com.blautech.reclutamiento.postulacion.Postulacion;
import com.blautech.reclutamiento.postulacion.PostulacionRepository;
import com.blautech.reclutamiento.vacante.VacanteHabilidad;
import com.blautech.reclutamiento.vacante.VacanteHabilidadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

// ============================================================================
// EL "MOTOR DE ANÁLISIS" -- ahora con dos motores posibles para la parte de
// habilidades:
//
//  1. IA (GeminiClient): le manda a Gemini la lista de habilidades que pide
//     la vacante y las que tiene el candidato, y es la IA la que decide, por
//     cada habilidad requerida, qué tanto la cumple (incluyendo habilidades
//     equivalentes o relacionadas, no solo coincidencia exacta de catálogo).
//  2. Reglas (el algoritmo original, en Java puro): compara por id de
//     habilidad y nivel, sin ningún tipo de "inteligencia" semántica.
//
// El motor de IA es el que se usa por default. Si Gemini no está
// configurado, está deshabilitado (gemini.api.habilitado=false), o la
// llamada falla por CUALQUIER motivo (sin internet, cuota agotada,
// respuesta mal formada, etc.), el sistema cae automáticamente al motor de
// reglas -- así un problema con la IA nunca tumba el análisis ni deja a
// RRHH sin resultado.
//
// Esto es exactamente la "mejora futura aislada" de la que hablaba el
// comentario original de este archivo: el Controller, las tablas
// (analisis_perfil, detalle_analisis) y todo lo que consume este resultado
// no cambiaron -- solo cambió CÓMO se calcula el porcentaje de cada
// criterio.
//
// Importante para el documento de tesis: la fórmula de ponderación final
// (obligatorias pesan el doble, umbrales 75/45 para ALTA/MEDIA/BAJA) sigue
// siendo 100% determinística, y es la MISMA fórmula sin importar qué motor
// generó los porcentajes por criterio -- eso es lo que se sigue pudiendo
// sustentar con una fórmula matemática exacta.
// ============================================================================
@Service
@RequiredArgsConstructor
public class AnalisisService {

    private final AnalisisPerfilRepository analisisPerfilRepository;
    private final DetalleAnalisisRepository detalleAnalisisRepository;
    private final PostulacionRepository postulacionRepository;
    private final CandidatoHabilidadRepository candidatoHabilidadRepository;
    private final VacanteHabilidadRepository vacanteHabilidadRepository;
    private final GeminiClient geminiClient;

    // Umbrales de clasificación -- ajustables según lo que decidas
    // sustentar en tu documento.
    private static final BigDecimal UMBRAL_ALTA = BigDecimal.valueOf(75);
    private static final BigDecimal UMBRAL_MEDIA = BigDecimal.valueOf(45);

    public AnalisisResponse generarAnalisis(Integer idPostulacion) {

        Postulacion postulacion = postulacionRepository.findById(idPostulacion)
                .orElseThrow(() -> new IllegalArgumentException("La postulación indicada no existe"));

        List<VacanteHabilidad> requeridas =
                vacanteHabilidadRepository.findByVacante_IdVacante(postulacion.getVacante().getIdVacante());

        List<CandidatoHabilidad> delCandidato =
                candidatoHabilidadRepository.findByCandidato_IdCandidato(postulacion.getCandidato().getIdCandidato());

        // Guardamos primero el encabezado del análisis (lo vamos a
        // actualizar con el porcentaje final una vez calculado).
        AnalisisPerfil analisis = new AnalisisPerfil();
        analisis.setPostulacion(postulacion);

        if (requeridas.isEmpty()) {
            analisis.setPorcentajeCoincidencia(BigDecimal.ZERO);
            analisis.setClasificacion("SIN_DATOS");
            analisis.setResumen("La vacante todavía no tiene habilidades requeridas registradas para comparar.");
            AnalisisPerfil guardado = analisisPerfilRepository.save(analisis);
            return new AnalisisResponse(guardado, List.of(), calcularExtras(requeridas, delCandidato));
        }

        // Primero intentamos que la IA evalúe la coincidencia de habilidades;
        // si no está disponible o falla, usamos el motor de reglas original
        // como respaldo. Los dos devuelven la lista de detalles EN EL MISMO
        // ORDEN que "requeridas", para que el cálculo de abajo (idéntico sin
        // importar el motor) los pueda emparejar por posición.
        List<DetalleAnalisis> detallesCalculados;
        boolean usoIA;
        if (geminiClient.estaDisponible()) {
            try {
                detallesCalculados = evaluarConIA(requeridas, delCandidato);
                usoIA = true;
            } catch (Exception e) {
                // Cualquier falla de la IA -- sin internet, API caída, cuota
                // agotada, respuesta mal formada -- no debe tumbar el
                // análisis. Se cae al motor de reglas y listo.
                System.err.println("[AnalisisService] Falló el análisis por IA, usando motor de reglas: "
                        + e.getMessage());
                detallesCalculados = evaluarPorReglas(requeridas, delCandidato);
                usoIA = false;
            }
        } else {
            detallesCalculados = evaluarPorReglas(requeridas, delCandidato);
            usoIA = false;
        }

        BigDecimal sumaPonderada = BigDecimal.ZERO;
        int sumaPesos = 0;

        for (int i = 0; i < requeridas.size(); i++) {
            VacanteHabilidad requerida = requeridas.get(i);
            BigDecimal porcentajeCriterio = detallesCalculados.get(i).getPorcentajeCoincidencia();

            // Las habilidades OBLIGATORIAS pesan el doble que las opcionales
            // al calcular el promedio general -- así una vacante con
            // requisitos obligatorios no cumplidos baja más el porcentaje.
            int peso = Boolean.TRUE.equals(requerida.getEsObligatoria()) ? 2 : 1;
            sumaPonderada = sumaPonderada.add(porcentajeCriterio.multiply(BigDecimal.valueOf(peso)));
            sumaPesos += peso;
        }

        BigDecimal porcentajeFinal = sumaPesos > 0
                ? sumaPonderada.divide(BigDecimal.valueOf(sumaPesos), 4, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        String clasificacion;
        if (porcentajeFinal.compareTo(UMBRAL_ALTA) >= 0) {
            clasificacion = "ALTA";
        } else if (porcentajeFinal.compareTo(UMBRAL_MEDIA) >= 0) {
            clasificacion = "MEDIA";
        } else {
            clasificacion = "BAJA";
        }

        analisis.setPorcentajeCoincidencia(porcentajeFinal);
        analisis.setClasificacion(clasificacion);
        analisis.setResumen(String.format(Locale.forLanguageTag("es-GT"),
                "El candidato cumple con un %.2f%% de las habilidades requeridas para esta vacante "
                        + "(clasificación: %s), evaluando %d criterio(s)%s.",
                porcentajeFinal, clasificacion, requeridas.size(),
                usoIA ? ", con análisis de habilidades asistido por IA" : ""));

        AnalisisPerfil guardado = analisisPerfilRepository.save(analisis);

        // Ahora sí guardamos los detalles, ya enlazados al análisis con id.
        for (DetalleAnalisis detalle : detallesCalculados) {
            detalle.setAnalisisPerfil(guardado);
            detalleAnalisisRepository.save(detalle);
        }

        return new AnalisisResponse(guardado, detallesCalculados, calcularExtras(requeridas, delCandidato));
    }

    public AnalisisResponse obtenerUltimoPorPostulacion(Integer idPostulacion) {
        List<AnalisisPerfil> analisis =
                analisisPerfilRepository.findByPostulacion_IdPostulacionOrderByFechaAnalisisDesc(idPostulacion);

        if (analisis.isEmpty()) {
            throw new IllegalArgumentException("Todavía no se ha generado un análisis para esta postulación");
        }

        AnalisisPerfil ultimo = analisis.get(0);
        List<DetalleAnalisis> detalles = detalleAnalisisRepository.findByAnalisisPerfil_IdAnalisis(ultimo.getIdAnalisis());

        // Las habilidades "extra" no se guardan en detalle_analisis (esa
        // tabla es el histórico del cálculo que ya se hizo) -- se
        // recalculan en vivo contra el perfil actual del candidato, así
        // siempre reflejan lo que tiene registrado ahora mismo, aunque
        // haya agregado habilidades nuevas después de generado el análisis.
        Postulacion postulacionDelAnalisis = ultimo.getPostulacion();
        List<VacanteHabilidad> requeridasActuales =
                vacanteHabilidadRepository.findByVacante_IdVacante(postulacionDelAnalisis.getVacante().getIdVacante());
        List<CandidatoHabilidad> delCandidatoActual =
                candidatoHabilidadRepository.findByCandidato_IdCandidato(postulacionDelAnalisis.getCandidato().getIdCandidato());

        return new AnalisisResponse(ultimo, detalles, calcularExtras(requeridasActuales, delCandidatoActual));
    }

    // ------------------------------------------------------------------
    // Habilidades del candidato que NO pidió la vacante -- informativas
    // para RRHH ("esto más sabe el candidato"), nunca entran al cálculo
    // del porcentaje de coincidencia ni a la clasificación.
    // ------------------------------------------------------------------
    private List<HabilidadExtra> calcularExtras(List<VacanteHabilidad> requeridas,
                                                  List<CandidatoHabilidad> delCandidato) {
        Set<Integer> idsRequeridos = requeridas.stream()
                .map(r -> r.getHabilidad().getIdHabilidad())
                .collect(Collectors.toSet());

        return delCandidato.stream()
                .filter(ch -> !idsRequeridos.contains(ch.getHabilidad().getIdHabilidad()))
                .map(ch -> new HabilidadExtra(
                        ch.getHabilidad().getNombre(),
                        ch.getNivel() != null ? ch.getNivel() : "Sin nivel especificado"))
                .toList();
    }

    // ------------------------------------------------------------------
    // Motor de REGLAS (el algoritmo original, sin IA): compara por id de
    // habilidad y nivel. Es el respaldo cuando la IA no está disponible.
    // ------------------------------------------------------------------
    private List<DetalleAnalisis> evaluarPorReglas(List<VacanteHabilidad> requeridas,
                                                     List<CandidatoHabilidad> delCandidato) {
        List<DetalleAnalisis> detalles = new ArrayList<>();

        for (VacanteHabilidad requerida : requeridas) {

            Optional<CandidatoHabilidad> coincidencia = delCandidato.stream()
                    .filter(ch -> ch.getHabilidad().getIdHabilidad().equals(requerida.getHabilidad().getIdHabilidad()))
                    .findFirst();

            BigDecimal porcentajeCriterio;
            String valorCandidato;
            String observacion;

            if (coincidencia.isPresent()) {
                CandidatoHabilidad ch = coincidencia.get();
                int nivelCandidato = rankNivel(ch.getNivel());
                int nivelRequerido = rankNivel(requerida.getNivelRequerido());

                if (nivelRequerido == 0 || nivelCandidato >= nivelRequerido) {
                    // El candidato cumple o supera el nivel pedido (o la
                    // vacante no especificó nivel -- basta con tener la habilidad).
                    porcentajeCriterio = BigDecimal.valueOf(100);
                    observacion = "Cumple el nivel requerido";
                } else if (nivelCandidato == 0) {
                    // Tiene la habilidad registrada pero sin nivel especificado.
                    porcentajeCriterio = BigDecimal.valueOf(50);
                    observacion = "Tiene la habilidad, sin nivel especificado";
                } else {
                    // Tiene la habilidad pero en un nivel menor al requerido:
                    // coincidencia parcial, proporcional a qué tan cerca está.
                    porcentajeCriterio = BigDecimal.valueOf(nivelCandidato)
                            .divide(BigDecimal.valueOf(nivelRequerido), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
                    observacion = "Nivel por debajo del requerido";
                }
                valorCandidato = (ch.getNivel() != null) ? ch.getNivel() : "Sin nivel especificado";
            } else {
                porcentajeCriterio = BigDecimal.ZERO;
                valorCandidato = "No registrada en el perfil del candidato";
                observacion = "El candidato no tiene esta habilidad registrada";
            }

            DetalleAnalisis detalle = new DetalleAnalisis();
            detalle.setCriterio(requerida.getHabilidad().getNombre());
            detalle.setValorEsperado(requerida.getNivelRequerido() != null
                    ? requerida.getNivelRequerido() : "Cualquier nivel");
            detalle.setValorCandidato(valorCandidato);
            detalle.setPorcentajeCoincidencia(porcentajeCriterio.setScale(2, RoundingMode.HALF_UP));
            detalle.setObservacion(observacion);
            detalles.add(detalle);
        }

        return detalles;
    }

    // ------------------------------------------------------------------
    // Motor de IA: le pasa a Gemini las habilidades de la vacante y del
    // candidato en texto plano, y usa su evaluación por criterio. Lanza
    // excepción (sin capturarla aquí) si algo sale mal -- quien llama a
    // este método es quien decide caer al motor de reglas.
    // ------------------------------------------------------------------
    private List<DetalleAnalisis> evaluarConIA(List<VacanteHabilidad> requeridas,
                                                 List<CandidatoHabilidad> delCandidato) {

        List<String> lineasVacante = new ArrayList<>();
        for (int i = 0; i < requeridas.size(); i++) {
            VacanteHabilidad r = requeridas.get(i);
            String nivelRequerido = r.getNivelRequerido() != null ? r.getNivelRequerido() : "cualquiera";
            lineasVacante.add(i + ": " + r.getHabilidad().getNombre()
                    + " - nivel requerido: " + nivelRequerido
                    + " - obligatoria: " + (Boolean.TRUE.equals(r.getEsObligatoria()) ? "sí" : "no"));
        }

        List<String> lineasCandidato = new ArrayList<>();
        for (CandidatoHabilidad ch : delCandidato) {
            String nivel = ch.getNivel() != null ? ch.getNivel() : "sin nivel especificado";
            lineasCandidato.add("- " + ch.getHabilidad().getNombre() + " (nivel: " + nivel + ")");
        }

        GeminiEvaluacionHabilidadesResponse respuesta =
                geminiClient.evaluarCoincidenciaHabilidades(lineasVacante, lineasCandidato);

        if (respuesta.getEvaluaciones() == null) {
            throw new IllegalStateException("La IA no devolvió ninguna evaluación");
        }

        Map<Integer, GeminiEvaluacionCriterio> porIndice = new HashMap<>();
        for (GeminiEvaluacionCriterio evaluacion : respuesta.getEvaluaciones()) {
            porIndice.put(evaluacion.getIndice(), evaluacion);
        }

        List<DetalleAnalisis> detalles = new ArrayList<>();
        for (int i = 0; i < requeridas.size(); i++) {
            VacanteHabilidad requerida = requeridas.get(i);
            GeminiEvaluacionCriterio evaluacion = porIndice.get(i);

            if (evaluacion == null) {
                // La IA se "saltó" una habilidad requerida -- tratamos la
                // respuesta completa como inválida, para no guardar un
                // análisis incompleto o inconsistente.
                throw new IllegalStateException(
                        "La IA no evaluó la habilidad #" + i + " (" + requerida.getHabilidad().getNombre() + ")");
            }

            BigDecimal porcentajeCriterio = evaluacion.getPorcentajeCoincidencia();
            if (porcentajeCriterio == null) {
                porcentajeCriterio = BigDecimal.ZERO;
            }
            // Nos protegemos de que la IA mande algo fuera de 0-100.
            if (porcentajeCriterio.compareTo(BigDecimal.ZERO) < 0) {
                porcentajeCriterio = BigDecimal.ZERO;
            } else if (porcentajeCriterio.compareTo(BigDecimal.valueOf(100)) > 0) {
                porcentajeCriterio = BigDecimal.valueOf(100);
            }

            DetalleAnalisis detalle = new DetalleAnalisis();
            detalle.setCriterio(requerida.getHabilidad().getNombre());
            detalle.setValorEsperado(requerida.getNivelRequerido() != null
                    ? requerida.getNivelRequerido() : "Cualquier nivel");
            detalle.setValorCandidato(evaluacion.getValorCandidato() != null
                    ? evaluacion.getValorCandidato() : "No especificado");
            detalle.setPorcentajeCoincidencia(porcentajeCriterio.setScale(2, RoundingMode.HALF_UP));
            detalle.setObservacion((evaluacion.getObservacion() != null
                    ? evaluacion.getObservacion() : "Evaluado por IA") + " (IA)");
            detalles.add(detalle);
        }

        return detalles;
    }

    // BASICO=1, INTERMEDIO=2, AVANZADO=3, cualquier otro valor (o null)=0.
    private int rankNivel(String nivel) {
        if (nivel == null) return 0;
        return switch (nivel.trim().toUpperCase()) {
            case "BASICO" -> 1;
            case "INTERMEDIO" -> 2;
            case "AVANZADO" -> 3;
            default -> 0;
        };
    }
}
