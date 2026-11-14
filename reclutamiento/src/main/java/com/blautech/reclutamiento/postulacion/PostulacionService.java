package com.blautech.reclutamiento.postulacion;


import com.blautech.reclutamiento.candidato.Candidato;
import com.blautech.reclutamiento.candidato.CandidatoRepository;
import com.blautech.reclutamiento.notificacion.N8nNotificador;
import com.blautech.reclutamiento.postulacion.dto.CambiarEstadoRequest;
import com.blautech.reclutamiento.postulacion.dto.PostulacionEstadisticasResponse;
import com.blautech.reclutamiento.postulacion.dto.PostulacionRequest;
import com.blautech.reclutamiento.postulacion.dto.PostulacionResponse;
import com.blautech.reclutamiento.usuario.Usuario;
import com.blautech.reclutamiento.usuario.UsuarioRepository;
import com.blautech.reclutamiento.vacante.Vacante;
import com.blautech.reclutamiento.vacante.VacanteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostulacionService {

    private final PostulacionRepository postulacionRepository;
    private final HistorialPostulacionRepository historialRepository;
    private final CandidatoRepository candidatoRepository;
    private final VacanteRepository vacanteRepository;
    private final UsuarioRepository usuarioRepository;
    private final N8nNotificador n8nNotificador;

    // ------------------------------------------------------------------
    // Candidato se postula a una vacante (RF09). Aquí van TODAS las
    // reglas de negocio que ya describiste en tu caso de uso CU02:
    // ------------------------------------------------------------------
    public PostulacionResponse postularse(String correoCandidato, PostulacionRequest request) {

        Usuario usuario = usuarioRepository.findByCorreo(correoCandidato)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));

        Candidato candidato = candidatoRepository.findByUsuario_IdUsuario(usuario.getIdUsuario())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Debes completar tu perfil de candidato antes de postularte (A1 del caso de uso CU02)"));

        Vacante vacante = vacanteRepository.findById(request.getIdVacante())
                .orElseThrow(() -> new IllegalArgumentException("La vacante indicada no existe"));

        // A2: si la vacante ya no está disponible, no se permite postular.
        if (!"ACTIVA".equals(vacante.getEstado())) {
            throw new IllegalStateException("Esta vacante ya no está disponible para postulaciones");
        }
        // A2b: si la vacante ya alcanzó su fecha de cierre, tampoco se permite postular,
        // aunque el estado todavía diga "ACTIVA" (cierre automático por fecha).
        if (vacante.getFechaCierre() != null && LocalDate.now().isAfter(vacante.getFechaCierre())) {
            throw new IllegalStateException("Esta vacante ya alcanzó su fecha de cierre y no acepta más postulaciones");
        }

        // A3: evitar postulaciones duplicadas (esto también lo protege la
        // restricción UNIQUE de la base de datos, pero validarlo aquí nos
        // permite devolver un mensaje claro en vez de un error SQL feo).
        if (postulacionRepository.existsByCandidato_IdCandidatoAndVacante_IdVacante(
                candidato.getIdCandidato(), vacante.getIdVacante())) {
            throw new IllegalArgumentException("Ya te postulaste anteriormente a esta vacante");
        }

        Postulacion postulacion = new Postulacion();
        postulacion.setCandidato(candidato);
        postulacion.setVacante(vacante);
        // estado ya nace en "RECIBIDA" por el valor por defecto de la entidad

        Postulacion guardada = postulacionRepository.save(postulacion);

        // Dejamos el primer registro en el historial, para que desde el
        // inicio quede trazabilidad de todos los cambios de estado.
        registrarHistorial(guardada, usuario, null, "RECIBIDA", "Postulación registrada");

        // Aviso opcional a n8n (correo de confirmación al candidato). Apagado
        // por defecto y a prueba de fallos: nunca lanza excepciones, así que
        // no puede romper la postulación.
        n8nNotificador.postulacionCreada(guardada);

        return new PostulacionResponse(guardada);
    }

    // "Consultar mis postulaciones" (RF-relacionado, parte del flujo del candidato)
    public List<PostulacionResponse> misPostulaciones(String correoCandidato) {
        Usuario usuario = usuarioRepository.findByCorreo(correoCandidato)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));

        Candidato candidato = candidatoRepository.findByUsuario_IdUsuario(usuario.getIdUsuario())
                .orElseThrow(() -> new IllegalArgumentException("No tienes un perfil de candidato creado"));

        return postulacionRepository.findByCandidato_IdCandidato(candidato.getIdCandidato()).stream()
                .map(PostulacionResponse::new)
                .collect(Collectors.toList());
    }

    // "Visualizar candidatos postulados a una vacante" (RF12, CU04, solo RRHH)
    public List<PostulacionResponse> listarPorVacante(Integer idVacante) {
        return postulacionRepository.findByVacante_IdVacante(idVacante).stream()
                .map(PostulacionResponse::new)
                .collect(Collectors.toList());
    }

    // RRHH consultando todas las postulaciones de UN candidato en
    // particular (se usa en el directorio de Candidatos, para saber a
    // cuántas vacantes se ha postulado y para abrir su detalle).
    public List<PostulacionResponse> listarPorCandidato(Integer idCandidato) {
        return postulacionRepository.findByCandidato_IdCandidato(idCandidato).stream()
                .map(PostulacionResponse::new)
                .collect(Collectors.toList());
    }

    // "Gestionar estado de las postulaciones" (RF15, CU04, solo RRHH)
    public PostulacionResponse cambiarEstado(String correoUsuarioRRHH, Integer idPostulacion,
                                             CambiarEstadoRequest request) {

        Usuario usuarioRRHH = usuarioRepository.findByCorreo(correoUsuarioRRHH)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));

        Postulacion postulacion = postulacionRepository.findById(idPostulacion)
                .orElseThrow(() -> new IllegalArgumentException("La postulación indicada no existe"));

        String estadoAnterior = postulacion.getEstado();
        postulacion.setEstado(request.getEstadoNuevo());
        Postulacion actualizada = postulacionRepository.save(postulacion);

        registrarHistorial(actualizada, usuarioRRHH, estadoAnterior, request.getEstadoNuevo(),
                request.getObservacion());

        return new PostulacionResponse(actualizada);
    }

    // El propio candidato puede retirar SU postulación, pero solo mientras
    // sigue en RECIBIDA -- en cuanto RRHH la toca (EN_REVISION en
    // adelante) ya hay trabajo humano invertido en ella, así que a partir
    // de ahí el candidato ya no puede hacerla desaparecer de la vista de
    // RRHH por su cuenta. RETIRADA es un valor de texto más para la misma
    // columna "estado" (varchar, sin CHECK en la base) -- no hace falta
    // ninguna migración para agregarlo.
    public PostulacionResponse retirar(String correoCandidato, Integer idPostulacion) {
        Usuario usuario = usuarioRepository.findByCorreo(correoCandidato)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));

        Candidato candidato = candidatoRepository.findByUsuario_IdUsuario(usuario.getIdUsuario())
                .orElseThrow(() -> new IllegalArgumentException("No tienes un perfil de candidato creado"));

        Postulacion postulacion = postulacionRepository.findById(idPostulacion)
                .orElseThrow(() -> new IllegalArgumentException("La postulación indicada no existe"));

        if (!postulacion.getCandidato().getIdCandidato().equals(candidato.getIdCandidato())) {
            throw new AccessDeniedException("No puedes retirar una postulación que no es tuya");
        }

        if (!"RECIBIDA".equals(postulacion.getEstado())) {
            throw new IllegalStateException(
                    "Ya no puedes retirar esta postulación porque RRHH ya empezó a revisarla");
        }

        String estadoAnterior = postulacion.getEstado();
        postulacion.setEstado("RETIRADA");
        Postulacion actualizada = postulacionRepository.save(postulacion);

        registrarHistorial(actualizada, usuario, estadoAnterior, "RETIRADA", "Retirada por el propio candidato");

        return new PostulacionResponse(actualizada);
    }

    // Antes este método no verificaba nada: CUALQUIER usuario autenticado
    // podía ver el historial de CUALQUIER postulación con solo cambiar el
    // número en la URL (ej. /postulaciones/7/historial), incluyendo las
    // observaciones internas que deja RRHH -- un candidato podía ver el
    // historial de otro candidato. Esto es un IDOR (Insecure Direct Object
    // Reference). La corrección: solo puede verlo RRHH, o el candidato
    // DUEÑO de esa postulación -- igual que ya se protege, por ejemplo,
    // eliminarHabilidad() en CandidatoService.
    public List<HistorialPostulacion> obtenerHistorial(String correoSolicitante, Integer idPostulacion) {
        Usuario solicitante = usuarioRepository.findByCorreo(correoSolicitante)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));

        Postulacion postulacion = postulacionRepository.findById(idPostulacion)
                .orElseThrow(() -> new IllegalArgumentException("La postulación indicada no existe"));

        boolean esRRHH = "RRHH".equals(solicitante.getRol().getNombre());

        boolean esElCandidatoDueño = candidatoRepository.findByUsuario_IdUsuario(solicitante.getIdUsuario())
                .map(c -> c.getIdCandidato().equals(postulacion.getCandidato().getIdCandidato()))
                .orElse(false);

        if (!esRRHH && !esElCandidatoDueño) {
            throw new AccessDeniedException("No tienes permiso para ver el historial de esta postulación");
        }

        return historialRepository.findByPostulacion_IdPostulacionOrderByFechaCambioAsc(idPostulacion);
    }

    // Datos agregados para el dashboard de RRHH (embudo de conversión y
    // tiempo promedio de contratación). Es una consulta de solo lectura;
    // se marca @Transactional para poder leer postulacion.getFechaPostulacion()
    // desde el historial (relación LAZY) sin tronar fuera de una sesión.
    @Transactional(readOnly = true)
    public PostulacionEstadisticasResponse obtenerEstadisticas() {
        Map<String, Long> porEstado = postulacionRepository.findAll().stream()
                .collect(Collectors.groupingBy(Postulacion::getEstado, Collectors.counting()));

        List<HistorialPostulacion> contrataciones = historialRepository.findByEstadoNuevo("CONTRATADO");

        List<Long> diasPorContratacion = contrataciones.stream()
                .map(h -> Duration.between(h.getPostulacion().getFechaPostulacion(), h.getFechaCambio()).toDays())
                .collect(Collectors.toList());

        Double promedioDias = diasPorContratacion.isEmpty()
                ? null
                : diasPorContratacion.stream().mapToLong(Long::longValue).average().orElse(0);

        return new PostulacionEstadisticasResponse(porEstado, promedioDias, contrataciones.size());
    }

    private void registrarHistorial(Postulacion postulacion, Usuario usuario,
                                    String estadoAnterior, String estadoNuevo, String observacion) {
        HistorialPostulacion historial = new HistorialPostulacion();
        historial.setPostulacion(postulacion);
        historial.setUsuario(usuario);
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo(estadoNuevo);
        historial.setObservacion(observacion);
        historialRepository.save(historial);
    }
}
