package com.blautech.reclutamiento.postulacion;

import com.blautech.reclutamiento.candidato.Candidato;
import com.blautech.reclutamiento.candidato.CandidatoRepository;
import com.blautech.reclutamiento.notificacion.N8nNotificador;
import com.blautech.reclutamiento.postulacion.dto.CambiarEstadoRequest;
import com.blautech.reclutamiento.postulacion.dto.PostulacionRequest;
import com.blautech.reclutamiento.postulacion.dto.PostulacionResponse;
import com.blautech.reclutamiento.rol.Rol;
import com.blautech.reclutamiento.usuario.Usuario;
import com.blautech.reclutamiento.usuario.UsuarioRepository;
import com.blautech.reclutamiento.vacante.Vacante;
import com.blautech.reclutamiento.vacante.VacanteRepository;
import org.springframework.security.access.AccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// ============================================================================
// Pruebas de PostulacionService: todas las reglas de negocio del CU02
// (postularse) y del RF15 (gestionar estado). Cada regla descrita en los
// comentarios A1/A2/A3 del propio Service tiene su prueba correspondiente.
// ============================================================================
@ExtendWith(MockitoExtension.class)
class PostulacionServiceTest {

    @Mock private PostulacionRepository postulacionRepository;
    @Mock private HistorialPostulacionRepository historialRepository;
    @Mock private CandidatoRepository candidatoRepository;
    @Mock private VacanteRepository vacanteRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private N8nNotificador n8nNotificador;

    @InjectMocks
    private PostulacionService postulacionService;

    private Usuario usuario;
    private Candidato candidato;
    private Vacante vacante;

    @BeforeEach
    void setUp() {
        Rol rolCandidato = new Rol();
        rolCandidato.setNombre("CANDIDATO");

        usuario = new Usuario();
        usuario.setIdUsuario(1);
        usuario.setNombre("Ana");
        usuario.setApellido("Pérez");
        usuario.setCorreo("ana@correo.com");
        usuario.setRol(rolCandidato);

        candidato = new Candidato();
        candidato.setIdCandidato(5);
        candidato.setUsuario(usuario);

        vacante = new Vacante();
        vacante.setIdVacante(10);
        vacante.setTitulo("Desarrollador Backend");
        vacante.setEstado("ACTIVA");
    }

    @Test
    @DisplayName("postularse guarda la postulación y registra el primer historial en RECIBIDA")
    void postularse_flujoExitoso_guardaYRegistraHistorial() {
        PostulacionRequest request = new PostulacionRequest();
        request.setIdVacante(10);

        when(usuarioRepository.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
        when(candidatoRepository.findByUsuario_IdUsuario(1)).thenReturn(Optional.of(candidato));
        when(vacanteRepository.findById(10)).thenReturn(Optional.of(vacante));
        when(postulacionRepository.existsByCandidato_IdCandidatoAndVacante_IdVacante(5, 10)).thenReturn(false);
        when(postulacionRepository.save(any(Postulacion.class))).thenAnswer(inv -> {
            Postulacion p = inv.getArgument(0);
            p.setIdPostulacion(100);
            return p;
        });

        PostulacionResponse resultado = postulacionService.postularse("ana@correo.com", request);

        assertThat(resultado.getIdPostulacion()).isEqualTo(100);
        assertThat(resultado.getEstado()).isEqualTo("RECIBIDA");

        ArgumentCaptor<HistorialPostulacion> captor = ArgumentCaptor.forClass(HistorialPostulacion.class);
        verify(historialRepository).save(captor.capture());
        assertThat(captor.getValue().getEstadoNuevo()).isEqualTo("RECIBIDA");
        assertThat(captor.getValue().getEstadoAnterior()).isNull();
    }

    @Test
    @DisplayName("postularse sin perfil de candidato lanza excepción (regla A1 del CU02)")
    void postularse_sinPerfilCandidato_lanzaExcepcion() {
        PostulacionRequest request = new PostulacionRequest();
        request.setIdVacante(10);

        when(usuarioRepository.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
        when(candidatoRepository.findByUsuario_IdUsuario(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postulacionService.postularse("ana@correo.com", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("completar tu perfil");

        verifyNoInteractions(postulacionRepository);
    }

    @Test
    @DisplayName("postularse a una vacante ya cerrada lanza excepción (regla A2)")
    void postularse_vacanteCerrada_lanzaExcepcion() {
        vacante.setEstado("CERRADA");
        PostulacionRequest request = new PostulacionRequest();
        request.setIdVacante(10);

        when(usuarioRepository.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
        when(candidatoRepository.findByUsuario_IdUsuario(1)).thenReturn(Optional.of(candidato));
        when(vacanteRepository.findById(10)).thenReturn(Optional.of(vacante));

        assertThatThrownBy(() -> postulacionService.postularse("ana@correo.com", request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya no está disponible");

        verify(postulacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("postularse dos veces a la misma vacante lanza excepción (regla A3, duplicados)")
    void postularse_yaPostuladoAntes_lanzaExcepcion() {
        PostulacionRequest request = new PostulacionRequest();
        request.setIdVacante(10);

        when(usuarioRepository.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
        when(candidatoRepository.findByUsuario_IdUsuario(1)).thenReturn(Optional.of(candidato));
        when(vacanteRepository.findById(10)).thenReturn(Optional.of(vacante));
        when(postulacionRepository.existsByCandidato_IdCandidatoAndVacante_IdVacante(5, 10)).thenReturn(true);

        assertThatThrownBy(() -> postulacionService.postularse("ana@correo.com", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya te postulaste");

        verify(postulacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("postularse a una vacante que no existe lanza excepción")
    void postularse_vacanteInexistente_lanzaExcepcion() {
        PostulacionRequest request = new PostulacionRequest();
        request.setIdVacante(999);

        when(usuarioRepository.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
        when(candidatoRepository.findByUsuario_IdUsuario(1)).thenReturn(Optional.of(candidato));
        when(vacanteRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postulacionService.postularse("ana@correo.com", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no existe");
    }

    @Test
    @DisplayName("cambiarEstado actualiza el estado y deja registro del estado anterior en el historial")
    void cambiarEstado_actualizaYRegistraHistorialConEstadoAnterior() {
        Postulacion postulacion = new Postulacion();
        postulacion.setIdPostulacion(100);
        postulacion.setCandidato(candidato);
        postulacion.setVacante(vacante);
        postulacion.setEstado("RECIBIDA");

        Usuario rrhh = new Usuario();
        rrhh.setIdUsuario(2);
        rrhh.setNombre("Luis");
        rrhh.setApellido("Gómez");

        CambiarEstadoRequest request = new CambiarEstadoRequest();
        request.setEstadoNuevo("EN_REVISION");
        request.setObservacion("Pasa a revisión técnica");

        when(usuarioRepository.findByCorreo("rrhh@correo.com")).thenReturn(Optional.of(rrhh));
        when(postulacionRepository.findById(100)).thenReturn(Optional.of(postulacion));
        when(postulacionRepository.save(any(Postulacion.class))).thenAnswer(inv -> inv.getArgument(0));

        PostulacionResponse resultado = postulacionService.cambiarEstado("rrhh@correo.com", 100, request);

        assertThat(resultado.getEstado()).isEqualTo("EN_REVISION");

        ArgumentCaptor<HistorialPostulacion> captor = ArgumentCaptor.forClass(HistorialPostulacion.class);
        verify(historialRepository).save(captor.capture());
        assertThat(captor.getValue().getEstadoAnterior()).isEqualTo("RECIBIDA");
        assertThat(captor.getValue().getEstadoNuevo()).isEqualTo("EN_REVISION");
    }

    @Test
    @DisplayName("cambiarEstado sobre una postulación inexistente lanza excepción")
    void cambiarEstado_postulacionInexistente_lanzaExcepcion() {
        Usuario rrhh = new Usuario();
        rrhh.setIdUsuario(2);

        CambiarEstadoRequest request = new CambiarEstadoRequest();
        request.setEstadoNuevo("EN_REVISION");

        when(usuarioRepository.findByCorreo("rrhh@correo.com")).thenReturn(Optional.of(rrhh));
        when(postulacionRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postulacionService.cambiarEstado("rrhh@correo.com", 999, request))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(historialRepository);
    }

    @Test
    @DisplayName("misPostulaciones lanza excepción si el usuario no tiene perfil de candidato")
    void misPostulaciones_sinPerfilCandidato_lanzaExcepcion() {
        when(usuarioRepository.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
        when(candidatoRepository.findByUsuario_IdUsuario(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postulacionService.misPostulaciones("ana@correo.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("perfil de candidato");
    }

    // ------------------------------------------------------------------
    // obtenerHistorial: corrige el IDOR (Insecure Direct Object Reference)
    // que antes dejaba ver el historial de CUALQUIER postulación a
    // CUALQUIER usuario autenticado, sin importar de quién fuera.
    // ------------------------------------------------------------------

    @Test
    @DisplayName("obtenerHistorial: RRHH SÍ puede ver el historial de una postulación que no es suya")
    void obtenerHistorial_usuarioRRHH_siPuedeVerCualquierHistorial() {
        Rol rolRRHH = new Rol();
        rolRRHH.setNombre("RRHH");

        Usuario usuarioRRHH = new Usuario();
        usuarioRRHH.setIdUsuario(99);
        usuarioRRHH.setCorreo("rrhh@correo.com");
        usuarioRRHH.setRol(rolRRHH);

        Postulacion postulacion = new Postulacion();
        postulacion.setIdPostulacion(100);
        postulacion.setCandidato(candidato); // la postulación es de OTRO candidato, no de RRHH

        when(usuarioRepository.findByCorreo("rrhh@correo.com")).thenReturn(Optional.of(usuarioRRHH));
        when(postulacionRepository.findById(100)).thenReturn(Optional.of(postulacion));
        when(historialRepository.findByPostulacion_IdPostulacionOrderByFechaCambioAsc(100))
                .thenReturn(java.util.List.of());

        postulacionService.obtenerHistorial("rrhh@correo.com", 100);

        verify(historialRepository).findByPostulacion_IdPostulacionOrderByFechaCambioAsc(100);
    }

    @Test
    @DisplayName("obtenerHistorial: el candidato DUEÑO de la postulación sí puede verlo")
    void obtenerHistorial_candidatoDueño_siPuedeVerlo() {
        Postulacion postulacion = new Postulacion();
        postulacion.setIdPostulacion(100);
        postulacion.setCandidato(candidato); // candidato del setUp(), id 5

        when(usuarioRepository.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
        when(postulacionRepository.findById(100)).thenReturn(Optional.of(postulacion));
        when(candidatoRepository.findByUsuario_IdUsuario(1)).thenReturn(Optional.of(candidato));
        when(historialRepository.findByPostulacion_IdPostulacionOrderByFechaCambioAsc(100))
                .thenReturn(java.util.List.of());

        postulacionService.obtenerHistorial("ana@correo.com", 100);

        verify(historialRepository).findByPostulacion_IdPostulacionOrderByFechaCambioAsc(100);
    }

    @Test
    @DisplayName("obtenerHistorial: un candidato que NO es el dueño de la postulación NO puede verlo (IDOR corregido)")
    void obtenerHistorial_candidatoQueNoEsElDueño_lanzaAccessDenied() {
        Candidato otroCandidato = new Candidato();
        otroCandidato.setIdCandidato(777); // distinto al candidato del setUp() (id 5)

        Postulacion postulacionDeOtroCandidato = new Postulacion();
        postulacionDeOtroCandidato.setIdPostulacion(100);
        postulacionDeOtroCandidato.setCandidato(otroCandidato);

        when(usuarioRepository.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
        when(postulacionRepository.findById(100)).thenReturn(Optional.of(postulacionDeOtroCandidato));
        when(candidatoRepository.findByUsuario_IdUsuario(1)).thenReturn(Optional.of(candidato));

        assertThatThrownBy(() -> postulacionService.obtenerHistorial("ana@correo.com", 100))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("permiso");

        verify(historialRepository, never()).findByPostulacion_IdPostulacionOrderByFechaCambioAsc(any());
    }
}
