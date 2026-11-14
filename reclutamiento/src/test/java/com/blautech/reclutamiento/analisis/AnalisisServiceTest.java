package com.blautech.reclutamiento.analisis;

import com.blautech.reclutamiento.analisis.dto.AnalisisResponse;
import com.blautech.reclutamiento.analisis.dto.HabilidadExtra;
import com.blautech.reclutamiento.analisis.ia.GeminiClient;
import com.blautech.reclutamiento.analisis.ia.GeminiEvaluacionCriterio;
import com.blautech.reclutamiento.analisis.ia.GeminiEvaluacionHabilidadesResponse;
import com.blautech.reclutamiento.candidato.Candidato;
import com.blautech.reclutamiento.candidato.CandidatoHabilidad;
import com.blautech.reclutamiento.candidato.CandidatoHabilidadRepository;
import com.blautech.reclutamiento.habilidad.Habilidad;
import com.blautech.reclutamiento.postulacion.Postulacion;
import com.blautech.reclutamiento.postulacion.PostulacionRepository;
import com.blautech.reclutamiento.vacante.Vacante;
import com.blautech.reclutamiento.vacante.VacanteHabilidad;
import com.blautech.reclutamiento.vacante.VacanteHabilidadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// ============================================================================
// Pruebas del "motor de análisis" (RF10, RF11, RF13). Es la pieza más
// importante para sustentar en la tesis, porque acá vive TODA la lógica de
// negocio del algoritmo de comparación -- por eso es la que más casos cubre.
//
// Estrategia: como AnalisisService no depende de Spring (solo de sus
// repositorios y de GeminiClient, que aquí van mockeados), no hace falta
// levantar el ApplicationContext -- basta con MockitoExtension. Esto hace
// que las pruebas corran en milisegundos y no necesiten base de datos NI
// internet.
//
// IMPORTANTE: como geminiClient es un mock, "geminiClient.estaDisponible()"
// devuelve false por default (comportamiento estándar de Mockito para
// métodos que no se stubean) -- por eso TODAS las pruebas que no mencionan
// a Gemini explícitamente siguen ejercitando el motor de reglas original,
// exactamente igual que antes de esta integración.
// ============================================================================
@ExtendWith(MockitoExtension.class)
class AnalisisServiceTest {

    @Mock private AnalisisPerfilRepository analisisPerfilRepository;
    @Mock private DetalleAnalisisRepository detalleAnalisisRepository;
    @Mock private PostulacionRepository postulacionRepository;
    @Mock private CandidatoHabilidadRepository candidatoHabilidadRepository;
    @Mock private VacanteHabilidadRepository vacanteHabilidadRepository;
    @Mock private GeminiClient geminiClient;

    @InjectMocks
    private AnalisisService analisisService;

    private Postulacion postulacion;
    private Vacante vacante;
    private Candidato candidato;

    @BeforeEach
    void setUp() {
        vacante = new Vacante();
        vacante.setIdVacante(10);

        candidato = new Candidato();
        candidato.setIdCandidato(20);

        postulacion = new Postulacion();
        postulacion.setIdPostulacion(1);
        postulacion.setVacante(vacante);
        postulacion.setCandidato(candidato);

        // save(...) de un repositorio JPA normalmente devuelve la misma
        // entidad que recibió (con el id ya asignado) -- lo simulamos así
        // para que el AnalisisPerfil que arma el service "sobreviva" el save.
        // Va en "lenient" porque no TODOS los tests de esta clase llegan a
        // llamar generarAnalisis() con éxito (algunos cortan antes, por una
        // excepción, o prueban obtenerUltimoPorPostulacion, que ni siquiera
        // usa este repositorio) -- sin lenient, Mockito en modo estricto
        // marca el stub como "no usado" en esos casos y la prueba truena
        // con UnnecessaryStubbingException aunque el resto del test esté bien.
        lenient().when(analisisPerfilRepository.save(any(AnalisisPerfil.class)))
                .thenAnswer(inv -> {
                    AnalisisPerfil a = inv.getArgument(0);
                    if (a.getIdAnalisis() == null) a.setIdAnalisis(99);
                    return a;
                });
    }

    private Habilidad habilidad(int id, String nombre) {
        Habilidad h = new Habilidad();
        h.setIdHabilidad(id);
        h.setNombre(nombre);
        return h;
    }

    private VacanteHabilidad requerida(Habilidad h, String nivel, boolean obligatoria) {
        VacanteHabilidad vh = new VacanteHabilidad();
        vh.setHabilidad(h);
        vh.setNivelRequerido(nivel);
        vh.setEsObligatoria(obligatoria);
        return vh;
    }

    private CandidatoHabilidad delCandidato(Habilidad h, String nivel) {
        CandidatoHabilidad ch = new CandidatoHabilidad();
        ch.setHabilidad(h);
        ch.setNivel(nivel);
        return ch;
    }

    @Test
    @DisplayName("generarAnalisis lanza excepción si la postulación no existe")
    void generarAnalisis_postulacionInexistente_lanzaExcepcion() {
        when(postulacionRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> analisisService.generarAnalisis(99))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no existe");

        verifyNoInteractions(analisisPerfilRepository);
    }

    @Test
    @DisplayName("Si la vacante no tiene habilidades requeridas, el resultado es SIN_DATOS y 0%")
    void generarAnalisis_sinHabilidadesRequeridas_devuelveSinDatos() {
        Habilidad java = habilidad(1, "Java");

        when(postulacionRepository.findById(1)).thenReturn(Optional.of(postulacion));
        when(vacanteHabilidadRepository.findByVacante_IdVacante(10)).thenReturn(List.of());
        when(candidatoHabilidadRepository.findByCandidato_IdCandidato(20))
                .thenReturn(List.of(delCandidato(java, "AVANZADO")));

        AnalisisResponse resultado = analisisService.generarAnalisis(1);

        assertThat(resultado.getClasificacion()).isEqualTo("SIN_DATOS");
        assertThat(resultado.getPorcentajeCoincidencia()).isEqualByComparingTo(BigDecimal.ZERO);
        verifyNoInteractions(detalleAnalisisRepository);

        // Como la vacante no pidió nada, TODO lo que trae el candidato
        // cuenta como "extra" -- informativo, no afecta el 0%/SIN_DATOS.
        assertThat(resultado.getHabilidadesExtra())
                .extracting(HabilidadExtra::nombre, HabilidadExtra::nivel)
                .containsExactly(tuple("Java", "AVANZADO"));
    }

    @Test
    @DisplayName("Candidato que cumple el nivel exacto en todas las habilidades obtiene 100% y clasificación ALTA")
    void generarAnalisis_cumpleTodo_devuelve100PorcientoAlta() {
        Habilidad java = habilidad(1, "Java");
        Habilidad sql = habilidad(2, "SQL");

        when(postulacionRepository.findById(1)).thenReturn(Optional.of(postulacion));
        when(vacanteHabilidadRepository.findByVacante_IdVacante(10)).thenReturn(List.of(
                requerida(java, "AVANZADO", true),
                requerida(sql, "INTERMEDIO", false)
        ));
        when(candidatoHabilidadRepository.findByCandidato_IdCandidato(20)).thenReturn(List.of(
                delCandidato(java, "AVANZADO"),
                delCandidato(sql, "INTERMEDIO")
        ));

        AnalisisResponse resultado = analisisService.generarAnalisis(1);

        assertThat(resultado.getPorcentajeCoincidencia()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(resultado.getClasificacion()).isEqualTo("ALTA");
        verify(detalleAnalisisRepository, times(2)).save(any(DetalleAnalisis.class));
    }

    @Test
    @DisplayName("Candidato sin ninguna de las habilidades requeridas obtiene 0% y clasificación BAJA")
    void generarAnalisis_sinNingunaHabilidad_devuelve0PorcientoBaja() {
        Habilidad java = habilidad(1, "Java");

        when(postulacionRepository.findById(1)).thenReturn(Optional.of(postulacion));
        when(vacanteHabilidadRepository.findByVacante_IdVacante(10))
                .thenReturn(List.of(requerida(java, "AVANZADO", true)));
        when(candidatoHabilidadRepository.findByCandidato_IdCandidato(20)).thenReturn(List.of());

        AnalisisResponse resultado = analisisService.generarAnalisis(1);

        assertThat(resultado.getPorcentajeCoincidencia()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resultado.getClasificacion()).isEqualTo("BAJA");
    }

    @Test
    @DisplayName("Una habilidad obligatoria incumplida pesa el doble que una opcional cumplida")
    void generarAnalisis_habilidadObligatoriaPesaElDoble() {
        Habilidad java = habilidad(1, "Java");   // obligatoria, NO la tiene -> 0%
        Habilidad git = habilidad(2, "Git");     // opcional, SÍ la tiene en el nivel pedido -> 100%

        when(postulacionRepository.findById(1)).thenReturn(Optional.of(postulacion));
        when(vacanteHabilidadRepository.findByVacante_IdVacante(10)).thenReturn(List.of(
                requerida(java, "INTERMEDIO", true),
                requerida(git, "BASICO", false)
        ));
        when(candidatoHabilidadRepository.findByCandidato_IdCandidato(20))
                .thenReturn(List.of(delCandidato(git, "BASICO")));

        AnalisisResponse resultado = analisisService.generarAnalisis(1);

        // Ponderado: (0*2 + 100*1) / (2+1) = 33.33
        assertThat(resultado.getPorcentajeCoincidencia()).isEqualByComparingTo(new BigDecimal("33.33"));
        assertThat(resultado.getClasificacion()).isEqualTo("BAJA");
    }

    @Test
    @DisplayName("Nivel del candidato por debajo del requerido da un porcentaje parcial proporcional")
    void generarAnalisis_nivelPorDebajo_calculaProporcional() {
        Habilidad java = habilidad(1, "Java");

        when(postulacionRepository.findById(1)).thenReturn(Optional.of(postulacion));
        when(vacanteHabilidadRepository.findByVacante_IdVacante(10))
                .thenReturn(List.of(requerida(java, "AVANZADO", true))); // rank 3
        when(candidatoHabilidadRepository.findByCandidato_IdCandidato(20))
                .thenReturn(List.of(delCandidato(java, "BASICO"))); // rank 1

        AnalisisResponse resultado = analisisService.generarAnalisis(1);

        // 1/3 * 100 = 33.33
        assertThat(resultado.getPorcentajeCoincidencia()).isEqualByComparingTo(new BigDecimal("33.33"));
    }

    @Test
    @DisplayName("Habilidad registrada sin nivel especificado obtiene coincidencia parcial de 50%")
    void generarAnalisis_habilidadSinNivel_devuelve50Porciento() {
        Habilidad java = habilidad(1, "Java");

        when(postulacionRepository.findById(1)).thenReturn(Optional.of(postulacion));
        when(vacanteHabilidadRepository.findByVacante_IdVacante(10))
                .thenReturn(List.of(requerida(java, "AVANZADO", true)));
        when(candidatoHabilidadRepository.findByCandidato_IdCandidato(20))
                .thenReturn(List.of(delCandidato(java, null)));

        AnalisisResponse resultado = analisisService.generarAnalisis(1);

        assertThat(resultado.getPorcentajeCoincidencia()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("El porcentaje exactamente en el umbral de 75 clasifica como ALTA (límite inclusive)")
    void generarAnalisis_umbralAlta_esInclusivo() {
        Habilidad java = habilidad(1, "Java");
        Habilidad sql = habilidad(2, "SQL");
        Habilidad git = habilidad(3, "Git");
        Habilidad linux = habilidad(4, "Linux"); // esta el candidato no la tiene -> 0%

        when(postulacionRepository.findById(1)).thenReturn(Optional.of(postulacion));
        when(vacanteHabilidadRepository.findByVacante_IdVacante(10)).thenReturn(List.of(
                requerida(java, "BASICO", false),
                requerida(sql, "BASICO", false),
                requerida(git, "BASICO", false),
                requerida(linux, "BASICO", false)
        ));
        when(candidatoHabilidadRepository.findByCandidato_IdCandidato(20)).thenReturn(List.of(
                delCandidato(java, "BASICO"),
                delCandidato(sql, "BASICO"),
                delCandidato(git, "BASICO")
        ));

        AnalisisResponse resultado = analisisService.generarAnalisis(1);

        // (100+100+100+0)/4 = 75.00 -> exactamente el umbral de ALTA
        assertThat(resultado.getPorcentajeCoincidencia()).isEqualByComparingTo(new BigDecimal("75.00"));
        assertThat(resultado.getClasificacion()).isEqualTo("ALTA");
    }

    // ------------------------------------------------------------------
    // Habilidades "extra" (RF nuevo): lo que el candidato sabe de más,
    // que la vacante no pidió. Es puramente informativo para RRHH y
    // nunca debe afectar el porcentaje ni la clasificación -- por eso
    // estas pruebas siempre validan ambas cosas por separado.
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Habilidades que el candidato tiene y la vacante no pidió se devuelven como extra, separadas de los detalles")
    void generarAnalisis_candidatoConHabilidadesExtra_lasSeparaDeLosDetalles() {
        Habilidad react = habilidad(1, "React");
        Habilidad python = habilidad(2, "Python");
        Habilidad docker = habilidad(3, "Docker"); // extra, no la pide la vacante
        Habilidad git = habilidad(4, "Git");       // extra, sin nivel especificado

        when(postulacionRepository.findById(1)).thenReturn(Optional.of(postulacion));
        when(vacanteHabilidadRepository.findByVacante_IdVacante(10)).thenReturn(List.of(
                requerida(react, "AVANZADO", true),
                requerida(python, null, false)
        ));
        when(candidatoHabilidadRepository.findByCandidato_IdCandidato(20)).thenReturn(List.of(
                delCandidato(react, "AVANZADO"),
                delCandidato(python, "INTERMEDIO"),
                delCandidato(docker, "INTERMEDIO"),
                delCandidato(git, null)
        ));

        AnalisisResponse resultado = analisisService.generarAnalisis(1);

        // El cálculo del porcentaje/clasificación no se entera de que
        // existen Docker y Git -- sigue dependiendo solo de lo pedido.
        assertThat(resultado.getPorcentajeCoincidencia()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(resultado.getClasificacion()).isEqualTo("ALTA");
        assertThat(resultado.getDetalles()).hasSize(2); // solo React y Python, lo pedido

        assertThat(resultado.getHabilidadesExtra())
                .extracting(HabilidadExtra::nombre, HabilidadExtra::nivel)
                .containsExactlyInAnyOrder(
                        tuple("Docker", "INTERMEDIO"),
                        tuple("Git", "Sin nivel especificado")
                );
    }

    @Test
    @DisplayName("Si el candidato no tiene nada fuera de lo pedido, habilidadesExtra queda vacía")
    void generarAnalisis_sinHabilidadesExtra_devuelveListaVacia() {
        Habilidad java = habilidad(1, "Java");

        when(postulacionRepository.findById(1)).thenReturn(Optional.of(postulacion));
        when(vacanteHabilidadRepository.findByVacante_IdVacante(10))
                .thenReturn(List.of(requerida(java, "INTERMEDIO", true)));
        when(candidatoHabilidadRepository.findByCandidato_IdCandidato(20))
                .thenReturn(List.of(delCandidato(java, "INTERMEDIO")));

        AnalisisResponse resultado = analisisService.generarAnalisis(1);

        assertThat(resultado.getHabilidadesExtra()).isEmpty();
    }

    @Test
    @DisplayName("obtenerUltimoPorPostulacion recalcula las habilidades extra contra el perfil ACTUAL del candidato")
    void obtenerUltimoPorPostulacion_recalculaExtrasContraPerfilActual() {
        Habilidad java = habilidad(1, "Java");
        Habilidad aws = habilidad(2, "AWS"); // el candidato la agregó DESPUÉS de generado el análisis

        AnalisisPerfil masReciente = new AnalisisPerfil();
        masReciente.setIdAnalisis(50);
        masReciente.setPostulacion(postulacion);
        masReciente.setPorcentajeCoincidencia(new BigDecimal("100.00"));
        masReciente.setClasificacion("ALTA");

        when(analisisPerfilRepository.findByPostulacion_IdPostulacionOrderByFechaAnalisisDesc(1))
                .thenReturn(List.of(masReciente));
        when(detalleAnalisisRepository.findByAnalisisPerfil_IdAnalisis(50)).thenReturn(List.of());

        when(vacanteHabilidadRepository.findByVacante_IdVacante(10))
                .thenReturn(List.of(requerida(java, "INTERMEDIO", true)));
        // Perfil actual del candidato: ya no solo tiene Java, también AWS --
        // algo que agregó después de que se generó este análisis.
        when(candidatoHabilidadRepository.findByCandidato_IdCandidato(20)).thenReturn(List.of(
                delCandidato(java, "INTERMEDIO"),
                delCandidato(aws, "BASICO")
        ));

        AnalisisResponse resultado = analisisService.obtenerUltimoPorPostulacion(1);

        // El porcentaje/clasificación del análisis histórico no cambia...
        assertThat(resultado.getPorcentajeCoincidencia()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(resultado.getClasificacion()).isEqualTo("ALTA");
        // ...pero las extra sí reflejan lo que el candidato tiene AHORA.
        assertThat(resultado.getHabilidadesExtra())
                .extracting(HabilidadExtra::nombre, HabilidadExtra::nivel)
                .containsExactly(tuple("AWS", "BASICO"));
    }

    @Test
    @DisplayName("Si Gemini está disponible y responde bien, se usan sus porcentajes de coincidencia")
    void generarAnalisis_iaDisponibleYResponde_usaPorcentajesDeLaIA() {
        Habilidad java = habilidad(1, "Java");

        when(postulacionRepository.findById(1)).thenReturn(Optional.of(postulacion));
        when(vacanteHabilidadRepository.findByVacante_IdVacante(10))
                .thenReturn(List.of(requerida(java, "AVANZADO", true)));
        // El candidato no tiene "Java" registrado tal cual, pero la IA
        // detecta algo relacionado (ej. Spring Boot) y le da crédito parcial
        // -- esto es justo lo que el motor de reglas, por comparar solo por
        // id de habilidad, NO puede hacer.
        when(candidatoHabilidadRepository.findByCandidato_IdCandidato(20)).thenReturn(List.of());

        when(geminiClient.estaDisponible()).thenReturn(true);

        GeminiEvaluacionCriterio evaluacion = new GeminiEvaluacionCriterio();
        evaluacion.setIndice(0);
        evaluacion.setPorcentajeCoincidencia(new BigDecimal("80"));
        evaluacion.setValorCandidato("Spring Boot (AVANZADO) -- relacionado con Java");
        evaluacion.setObservacion("Tiene una habilidad estrechamente relacionada");

        GeminiEvaluacionHabilidadesResponse respuestaIA = new GeminiEvaluacionHabilidadesResponse();
        respuestaIA.setEvaluaciones(List.of(evaluacion));

        when(geminiClient.evaluarCoincidenciaHabilidades(anyList(), anyList())).thenReturn(respuestaIA);

        AnalisisResponse resultado = analisisService.generarAnalisis(1);

        assertThat(resultado.getPorcentajeCoincidencia()).isEqualByComparingTo(new BigDecimal("80.00"));
        assertThat(resultado.getResumen()).contains("asistido por IA");
        assertThat(resultado.getDetalles().get(0).getObservacion()).contains("(IA)");
    }

    @Test
    @DisplayName("Si Gemini falla, el análisis cae automáticamente al motor de reglas sin tumbarse")
    void generarAnalisis_iaFalla_caeAMotorDeReglas() {
        Habilidad java = habilidad(1, "Java");

        when(postulacionRepository.findById(1)).thenReturn(Optional.of(postulacion));
        when(vacanteHabilidadRepository.findByVacante_IdVacante(10))
                .thenReturn(List.of(requerida(java, "AVANZADO", true)));
        when(candidatoHabilidadRepository.findByCandidato_IdCandidato(20))
                .thenReturn(List.of(delCandidato(java, "AVANZADO")));

        when(geminiClient.estaDisponible()).thenReturn(true);
        when(geminiClient.evaluarCoincidenciaHabilidades(anyList(), anyList()))
                .thenThrow(new IllegalStateException("sin internet"));

        AnalisisResponse resultado = analisisService.generarAnalisis(1);

        // Aunque la IA truena, el candidato SÍ cumple por reglas -> 100% ALTA.
        assertThat(resultado.getPorcentajeCoincidencia()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(resultado.getClasificacion()).isEqualTo("ALTA");
        assertThat(resultado.getResumen()).doesNotContain("asistido por IA");
    }

    @Test
    @DisplayName("obtenerUltimoPorPostulacion lanza excepción si nunca se ha generado un análisis")
    void obtenerUltimoPorPostulacion_sinAnalisisPrevio_lanzaExcepcion() {
        when(analisisPerfilRepository.findByPostulacion_IdPostulacionOrderByFechaAnalisisDesc(5))
                .thenReturn(List.of());

        assertThatThrownBy(() -> analisisService.obtenerUltimoPorPostulacion(5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Todavía no se ha generado");
    }

    @Test
    @DisplayName("obtenerUltimoPorPostulacion devuelve el análisis más reciente con sus detalles")
    void obtenerUltimoPorPostulacion_devuelveElMasReciente() {
        AnalisisPerfil masReciente = new AnalisisPerfil();
        masReciente.setIdAnalisis(50);
        masReciente.setPostulacion(postulacion);
        masReciente.setPorcentajeCoincidencia(new BigDecimal("80.00"));
        masReciente.setClasificacion("ALTA");

        when(analisisPerfilRepository.findByPostulacion_IdPostulacionOrderByFechaAnalisisDesc(1))
                .thenReturn(List.of(masReciente));
        when(detalleAnalisisRepository.findByAnalisisPerfil_IdAnalisis(50)).thenReturn(List.of());

        AnalisisResponse resultado = analisisService.obtenerUltimoPorPostulacion(1);

        assertThat(resultado.getIdAnalisis()).isEqualTo(50);
        assertThat(resultado.getClasificacion()).isEqualTo("ALTA");
    }
}
