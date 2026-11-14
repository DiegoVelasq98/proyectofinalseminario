package com.blautech.reclutamiento.vacante;

import com.blautech.reclutamiento.area.Area;
import com.blautech.reclutamiento.area.AreaRepository;
import com.blautech.reclutamiento.habilidad.Habilidad;
import com.blautech.reclutamiento.habilidad.HabilidadRepository;
import com.blautech.reclutamiento.usuario.Usuario;
import com.blautech.reclutamiento.usuario.UsuarioRepository;
import com.blautech.reclutamiento.vacante.dto.AsignarResponsableRequest;
import com.blautech.reclutamiento.vacante.dto.ResponsableResponse;
import com.blautech.reclutamiento.vacante.dto.VacanteHabilidadRequest;
import com.blautech.reclutamiento.vacante.dto.VacanteRequest;
import com.blautech.reclutamiento.vacante.dto.VacanteResponse;
import com.blautech.reclutamiento.vacante.dto.VacanteHabilidadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VacanteService {

    private final VacanteRepository vacanteRepository;
    private final VacanteHabilidadRepository vacanteHabilidadRepository;
    private final UsuarioRepository usuarioRepository;
    private final HabilidadRepository habilidadRepository;
    private final AreaRepository areaRepository;

    public VacanteResponse crear(String correoCreador, VacanteRequest request) {
        Usuario creador = usuarioRepository.findByCorreo(correoCreador)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));

        Vacante vacante = new Vacante();
        vacante.setUsuarioCreador(creador);
        vacante.setTitulo(request.getTitulo());
        vacante.setDescripcion(request.getDescripcion());
        vacante.setConocimientosRequeridos(request.getConocimientosRequeridos());
        vacante.setUbicacion(request.getUbicacion());
        vacante.setModalidad(request.getModalidad());
        vacante.setExperienciaRequerida(request.getExperienciaRequerida());
        vacante.setNivelAcademico(request.getNivelAcademico());
        vacante.setFechaCierre(request.getFechaCierre());
        vacante.setArea(resolverArea(request.getIdArea()));

        // Por defecto una vacante nace ACTIVA (comportamiento de siempre).
        // Si RRHH eligió explícitamente "Borrador" al crearla, respeta eso.
        if ("BORRADOR".equalsIgnoreCase(request.getEstadoInicial())) {
            vacante.setEstado("BORRADOR");
        }

        Vacante guardada = vacanteRepository.save(vacante);
        return armarRespuesta(guardada);
    }

    // Lista solo las ACTIVAS -- es lo que ve un candidato al consultar
    // "vacantes disponibles" (RF05 de tu tabla de requerimientos).
    public List<VacanteResponse> listarActivas() {
        return vacanteRepository.findByEstado("ACTIVA").stream()
                .map(this::armarRespuesta)
                .collect(Collectors.toList());
    }

    // Lista TODAS (para que RRHH administre, incluidas borrador/cerradas).
    public List<VacanteResponse> listarTodas() {
        return vacanteRepository.findAll().stream()
                .map(this::armarRespuesta)
                .collect(Collectors.toList());
    }

    public VacanteResponse obtener(Integer idVacante) {
        Vacante vacante = buscarPorId(idVacante);
        return armarRespuesta(vacante);
    }

    public VacanteResponse actualizar(Integer idVacante, VacanteRequest request) {
        Vacante vacante = buscarPorId(idVacante);

        vacante.setTitulo(request.getTitulo());
        vacante.setDescripcion(request.getDescripcion());
        vacante.setConocimientosRequeridos(request.getConocimientosRequeridos());
        vacante.setUbicacion(request.getUbicacion());
        vacante.setModalidad(request.getModalidad());
        vacante.setExperienciaRequerida(request.getExperienciaRequerida());
        vacante.setNivelAcademico(request.getNivelAcademico());
        vacante.setFechaCierre(request.getFechaCierre());
        vacante.setArea(resolverArea(request.getIdArea()));

        return armarRespuesta(vacanteRepository.save(vacante));
    }

    public VacanteResponse cerrar(Integer idVacante) {
        Vacante vacante = buscarPorId(idVacante);
        vacante.setEstado("CERRADA");
        return armarRespuesta(vacanteRepository.save(vacante));
    }

    // Pasa una vacante de BORRADOR a ACTIVA (publicarla).
    public VacanteResponse publicar(Integer idVacante) {
        Vacante vacante = buscarPorId(idVacante);
        vacante.setEstado("ACTIVA");
        return armarRespuesta(vacanteRepository.save(vacante));
    }

    // Lista los usuarios RRHH activos que se pueden asignar como
    // responsables de una vacante (para llenar el <select> del frontend).
    public List<ResponsableResponse> listarResponsables() {
        return usuarioRepository.findByRol_NombreAndEstado("RRHH", "ACTIVO").stream()
                .map(ResponsableResponse::new)
                .collect(Collectors.toList());
    }

    // Reasigna "el responsable" de una vacante. Reutiliza el campo
    // id_usuario_creador que ya existía en la tabla vacante (antes solo se
    // llenaba una vez, al crearla) -- así no hace falta agregar ninguna
    // columna nueva a la base de datos para soportar reasignación.
    public VacanteResponse asignarResponsable(Integer idVacante, AsignarResponsableRequest request) {
        Vacante vacante = buscarPorId(idVacante);

        Usuario nuevoResponsable = usuarioRepository.findById(request.getIdUsuarioResponsable())
                .orElseThrow(() -> new IllegalArgumentException("El usuario indicado no existe"));

        if (!"RRHH".equals(nuevoResponsable.getRol().getNombre())) {
            throw new IllegalArgumentException("El responsable de una vacante debe tener rol RRHH");
        }

        vacante.setUsuarioCreador(nuevoResponsable);
        return armarRespuesta(vacanteRepository.save(vacante));
    }

    public VacanteHabilidadResponse agregarHabilidadRequerida(Integer idVacante, VacanteHabilidadRequest request) {
        Vacante vacante = buscarPorId(idVacante);

        Habilidad habilidad = habilidadRepository.findById(request.getIdHabilidad())
                .orElseThrow(() -> new IllegalArgumentException("La habilidad indicada no existe"));

        if (vacanteHabilidadRepository.existsByVacante_IdVacanteAndHabilidad_IdHabilidad(
                idVacante, habilidad.getIdHabilidad())) {
            throw new IllegalArgumentException("Esta habilidad ya está registrada para la vacante");
        }

        VacanteHabilidad vh = new VacanteHabilidad();
        vh.setVacante(vacante);
        vh.setHabilidad(habilidad);
        vh.setNivelRequerido(request.getNivelRequerido());
        vh.setEsObligatoria(request.isEsObligatoria());

        VacanteHabilidad guardada = vacanteHabilidadRepository.save(vh);
        return new VacanteHabilidadResponse(guardada);
    }

    private Area resolverArea(Integer idArea) {
        if (idArea == null) return null;
        return areaRepository.findById(idArea)
                .orElseThrow(() -> new IllegalArgumentException("El área indicada no existe"));
    }

    private Vacante buscarPorId(Integer idVacante) {
        return vacanteRepository.findById(idVacante)
                .orElseThrow(() -> new IllegalArgumentException("La vacante indicada no existe"));
    }

    private VacanteResponse armarRespuesta(Vacante vacante) {
        var habilidades = vacanteHabilidadRepository.findByVacante_IdVacante(vacante.getIdVacante());
        return new VacanteResponse(vacante, habilidades);
    }
}
