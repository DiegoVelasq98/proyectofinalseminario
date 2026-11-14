package com.blautech.reclutamiento.candidato;

import com.blautech.reclutamiento.candidato.dto.ActualizarNivelHabilidadRequest;
import com.blautech.reclutamiento.candidato.dto.CandidatoHabilidadRequest;
import com.blautech.reclutamiento.candidato.dto.CandidatoHabilidadResponse;
import com.blautech.reclutamiento.candidato.dto.CandidatoPerfilRequest;
import com.blautech.reclutamiento.candidato.dto.CandidatoPerfilResponse;
import com.blautech.reclutamiento.habilidad.Habilidad;
import com.blautech.reclutamiento.habilidad.HabilidadRepository;
import com.blautech.reclutamiento.usuario.Usuario;
import com.blautech.reclutamiento.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// ============================================================================
// Pruebas de CandidatoService: RF06 (perfil del candidato) y la parte de
// habilidades que consume el RRHH y el motor de análisis (RF04/RF10).
// ============================================================================
@ExtendWith(MockitoExtension.class)
class CandidatoServiceTest {

    @Mock private CandidatoRepository candidatoRepository;
    @Mock private CurriculumRepository curriculumRepository;
    @Mock private ExperienciaLaboralRepository experienciaLaboralRepository;
    @Mock private EducacionRepository educacionRepository;
    @Mock private CandidatoHabilidadRepository candidatoHabilidadRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private HabilidadRepository habilidadRepository;

    @InjectMocks
    private CandidatoService candidatoService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setIdUsuario(1);
        usuario.setNombre("Ana");
        usuario.setApellido("Pérez");
        usuario.setCorreo("ana@correo.com");
    }

    @Test
    @DisplayName("crearPerfil cuando el usuario ya tiene un perfil de candidato lanza excepción")
    void crearPerfil_yaExiste_lanzaExcepcion() {
        when(usuarioRepository.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
        when(candidatoRepository.existsByUsuario_IdUsuario(1)).thenReturn(true);

        assertThatThrownBy(() -> candidatoService.crearPerfil("ana@correo.com", new CandidatoPerfilRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya tiene un perfil");

        verify(candidatoRepository, never()).save(any());
    }

    @Test
    @DisplayName("crearPerfil exitoso guarda los datos del candidato asociados al usuario")
    void crearPerfil_flujoExitoso_guardaDatos() {
        CandidatoPerfilRequest request = new CandidatoPerfilRequest();
        request.setFechaNacimiento(LocalDate.of(1998, 5, 20));
        request.setCiudad("Guatemala");
        request.setPais("Guatemala");

        when(usuarioRepository.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
        when(candidatoRepository.existsByUsuario_IdUsuario(1)).thenReturn(false);
        when(candidatoRepository.save(any(Candidato.class))).thenAnswer(inv -> {
            Candidato c = inv.getArgument(0);
            c.setIdCandidato(5);
            return c;
        });
        when(experienciaLaboralRepository.findByCandidato_IdCandidato(5)).thenReturn(List.of());
        when(educacionRepository.findByCandidato_IdCandidato(5)).thenReturn(List.of());
        when(candidatoHabilidadRepository.findByCandidato_IdCandidato(5)).thenReturn(List.of());

        CandidatoPerfilResponse resultado = candidatoService.crearPerfil("ana@correo.com", request);

        assertThat(resultado.getCiudad()).isEqualTo("Guatemala");
        assertThat(resultado.getNombre()).isEqualTo("Ana");
    }

    @Test
    @DisplayName("obtenerPerfil de un usuario sin perfil de candidato lanza excepción")
    void obtenerPerfil_sinPerfilCreado_lanzaExcepcion() {
        when(usuarioRepository.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
        when(candidatoRepository.findByUsuario_IdUsuario(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> candidatoService.obtenerPerfil("ana@correo.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no tiene un perfil de candidato");
    }

    @Test
    @DisplayName("agregarHabilidad duplicada para el mismo candidato lanza excepción")
    void agregarHabilidad_duplicada_lanzaExcepcion() {
        Candidato candidato = new Candidato();
        candidato.setIdCandidato(5);
        candidato.setUsuario(usuario);

        Habilidad java = new Habilidad();
        java.setIdHabilidad(1);
        java.setNombre("Java");

        CandidatoHabilidadRequest request = new CandidatoHabilidadRequest();
        request.setIdHabilidad(1);

        when(usuarioRepository.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
        when(candidatoRepository.findByUsuario_IdUsuario(1)).thenReturn(Optional.of(candidato));
        when(habilidadRepository.findById(1)).thenReturn(Optional.of(java));
        when(candidatoHabilidadRepository.existsByCandidato_IdCandidatoAndHabilidad_IdHabilidad(5, 1))
                .thenReturn(true);

        assertThatThrownBy(() -> candidatoService.agregarHabilidad("ana@correo.com", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya registraste esta habilidad");

        verify(candidatoHabilidadRepository, never()).save(any());
    }

    @Test
    @DisplayName("agregarHabilidad con una habilidad que no existe en el catálogo lanza excepción")
    void agregarHabilidad_habilidadInexistente_lanzaExcepcion() {
        Candidato candidato = new Candidato();
        candidato.setIdCandidato(5);
        candidato.setUsuario(usuario);

        CandidatoHabilidadRequest request = new CandidatoHabilidadRequest();
        request.setIdHabilidad(999);

        when(usuarioRepository.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
        when(candidatoRepository.findByUsuario_IdUsuario(1)).thenReturn(Optional.of(candidato));
        when(habilidadRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> candidatoService.agregarHabilidad("ana@correo.com", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no existe");
    }

    @Test
    @DisplayName("agregarHabilidad exitosa asocia la habilidad al candidato con su nivel")
    void agregarHabilidad_flujoExitoso_asociaLaHabilidad() {
        Candidato candidato = new Candidato();
        candidato.setIdCandidato(5);
        candidato.setUsuario(usuario);

        Habilidad java = new Habilidad();
        java.setIdHabilidad(1);
        java.setNombre("Java");

        CandidatoHabilidadRequest request = new CandidatoHabilidadRequest();
        request.setIdHabilidad(1);
        request.setNivel("AVANZADO");

        when(usuarioRepository.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
        when(candidatoRepository.findByUsuario_IdUsuario(1)).thenReturn(Optional.of(candidato));
        when(habilidadRepository.findById(1)).thenReturn(Optional.of(java));
        when(candidatoHabilidadRepository.existsByCandidato_IdCandidatoAndHabilidad_IdHabilidad(5, 1))
                .thenReturn(false);
        when(candidatoHabilidadRepository.save(any(CandidatoHabilidad.class))).thenAnswer(inv -> inv.getArgument(0));

        CandidatoHabilidadResponse resultado = candidatoService.agregarHabilidad("ana@correo.com", request);

        assertThat(resultado.getNombreHabilidad()).isEqualTo("Java");
        assertThat(resultado.getNivel()).isEqualTo("AVANZADO");
    }

    @Test
    @DisplayName("actualizarNivelHabilidad sobre una habilidad de OTRO candidato lanza excepción")
    void actualizarNivelHabilidad_noPertenece_lanzaExcepcion() {
        Candidato candidato = new Candidato();
        candidato.setIdCandidato(5);
        candidato.setUsuario(usuario);

        Candidato otroCandidato = new Candidato();
        otroCandidato.setIdCandidato(99);

        Habilidad java = new Habilidad();
        java.setIdHabilidad(1);
        java.setNombre("Java");

        CandidatoHabilidad ch = new CandidatoHabilidad();
        ch.setIdCandidatoHabilidad(10);
        ch.setCandidato(otroCandidato);
        ch.setHabilidad(java);

        ActualizarNivelHabilidadRequest request = new ActualizarNivelHabilidadRequest();
        request.setNivel("AVANZADO");

        when(usuarioRepository.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
        when(candidatoRepository.findByUsuario_IdUsuario(1)).thenReturn(Optional.of(candidato));
        when(candidatoHabilidadRepository.findById(10)).thenReturn(Optional.of(ch));

        assertThatThrownBy(() -> candidatoService.actualizarNivelHabilidad("ana@correo.com", 10, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no pertenece a tu perfil");

        verify(candidatoHabilidadRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizarNivelHabilidad con un nivel fuera del catálogo lanza excepción")
    void actualizarNivelHabilidad_nivelInvalido_lanzaExcepcion() {
        Candidato candidato = new Candidato();
        candidato.setIdCandidato(5);
        candidato.setUsuario(usuario);

        Habilidad java = new Habilidad();
        java.setIdHabilidad(1);
        java.setNombre("Java");

        CandidatoHabilidad ch = new CandidatoHabilidad();
        ch.setIdCandidatoHabilidad(10);
        ch.setCandidato(candidato);
        ch.setHabilidad(java);

        ActualizarNivelHabilidadRequest request = new ActualizarNivelHabilidadRequest();
        request.setNivel("EXPERTO"); // no existe en el catálogo de niveles

        when(usuarioRepository.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
        when(candidatoRepository.findByUsuario_IdUsuario(1)).thenReturn(Optional.of(candidato));
        when(candidatoHabilidadRepository.findById(10)).thenReturn(Optional.of(ch));

        assertThatThrownBy(() -> candidatoService.actualizarNivelHabilidad("ana@correo.com", 10, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BASICO, INTERMEDIO o AVANZADO");

        verify(candidatoHabilidadRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizarNivelHabilidad exitoso le pone el nivel a una habilidad que no tenía (ej. detectada del currículum)")
    void actualizarNivelHabilidad_flujoExitoso_lePoneElNivel() {
        Candidato candidato = new Candidato();
        candidato.setIdCandidato(5);
        candidato.setUsuario(usuario);

        Habilidad java = new Habilidad();
        java.setIdHabilidad(1);
        java.setNombre("Java");

        CandidatoHabilidad ch = new CandidatoHabilidad();
        ch.setIdCandidatoHabilidad(10);
        ch.setCandidato(candidato);
        ch.setHabilidad(java);
        ch.setNivel(null); // así queda cuando la detecta el currículum

        ActualizarNivelHabilidadRequest request = new ActualizarNivelHabilidadRequest();
        request.setNivel("INTERMEDIO");

        when(usuarioRepository.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
        when(candidatoRepository.findByUsuario_IdUsuario(1)).thenReturn(Optional.of(candidato));
        when(candidatoHabilidadRepository.findById(10)).thenReturn(Optional.of(ch));
        when(candidatoHabilidadRepository.save(any(CandidatoHabilidad.class))).thenAnswer(inv -> inv.getArgument(0));

        CandidatoHabilidadResponse resultado = candidatoService.actualizarNivelHabilidad("ana@correo.com", 10, request);

        assertThat(resultado.getNivel()).isEqualTo("INTERMEDIO");
        assertThat(ch.getNivel()).isEqualTo("INTERMEDIO");
    }
}
