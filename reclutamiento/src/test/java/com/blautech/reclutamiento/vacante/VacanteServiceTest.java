package com.blautech.reclutamiento.vacante;

import com.blautech.reclutamiento.habilidad.Habilidad;
import com.blautech.reclutamiento.habilidad.HabilidadRepository;
import com.blautech.reclutamiento.usuario.Usuario;
import com.blautech.reclutamiento.usuario.UsuarioRepository;
import com.blautech.reclutamiento.vacante.dto.VacanteHabilidadRequest;
import com.blautech.reclutamiento.vacante.dto.VacanteHabilidadResponse;
import com.blautech.reclutamiento.area.AreaRepository;
import com.blautech.reclutamiento.vacante.dto.VacanteRequest;
import com.blautech.reclutamiento.vacante.dto.VacanteResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// ============================================================================
// Pruebas de VacanteService: RF03 (CRUD de vacantes) y RF04 (habilidades
// requeridas por vacante).
// ============================================================================
@ExtendWith(MockitoExtension.class)
class VacanteServiceTest {

    @Mock private VacanteRepository vacanteRepository;
    @Mock private VacanteHabilidadRepository vacanteHabilidadRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private HabilidadRepository habilidadRepository;
    @Mock private AreaRepository areaRepository; // VacanteService la necesita en su constructor aunque estas pruebas no la usen directamente

    @InjectMocks
    private VacanteService vacanteService;

    private Usuario creador;

    @BeforeEach
    void setUp() {
        creador = new Usuario();
        creador.setIdUsuario(1);
        creador.setNombre("Luis");
        creador.setApellido("Gómez");
        creador.setCorreo("rrhh@correo.com");
    }

    @Test
    @DisplayName("crear registra la vacante asociada al usuario RRHH que la crea")
    void crear_flujoExitoso_asociaAlCreador() {
        VacanteRequest request = new VacanteRequest();
        request.setTitulo("Desarrollador Backend");
        request.setModalidad("REMOTO");

        when(usuarioRepository.findByCorreo("rrhh@correo.com")).thenReturn(Optional.of(creador));
        when(vacanteRepository.save(any(Vacante.class))).thenAnswer(inv -> {
            Vacante v = inv.getArgument(0);
            v.setIdVacante(10);
            return v;
        });
        when(vacanteHabilidadRepository.findByVacante_IdVacante(10)).thenReturn(List.of());

        VacanteResponse resultado = vacanteService.crear("rrhh@correo.com", request);

        assertThat(resultado.getTitulo()).isEqualTo("Desarrollador Backend");
        assertThat(resultado.getCreadoPor()).isEqualTo("Luis Gómez");
    }

    @Test
    @DisplayName("listarActivas solo devuelve vacantes en estado ACTIVA")
    void listarActivas_filtraPorEstadoActiva() {
        Vacante activa = new Vacante();
        activa.setIdVacante(1);
        activa.setEstado("ACTIVA");
        activa.setUsuarioCreador(creador);

        when(vacanteRepository.findByEstado("ACTIVA")).thenReturn(List.of(activa));
        when(vacanteHabilidadRepository.findByVacante_IdVacante(1)).thenReturn(List.of());

        List<VacanteResponse> resultado = vacanteService.listarActivas();

        assertThat(resultado).hasSize(1);
        verify(vacanteRepository, never()).findAll();
    }

    @Test
    @DisplayName("actualizar una vacante inexistente lanza excepción")
    void actualizar_vacanteInexistente_lanzaExcepcion() {
        when(vacanteRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vacanteService.actualizar(999, new VacanteRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no existe");
    }

    @Test
    @DisplayName("cerrar cambia el estado de la vacante a CERRADA")
    void cerrar_cambiaEstadoACerrada() {
        Vacante vacante = new Vacante();
        vacante.setIdVacante(10);
        vacante.setEstado("ACTIVA");
        vacante.setUsuarioCreador(creador);

        when(vacanteRepository.findById(10)).thenReturn(Optional.of(vacante));
        when(vacanteRepository.save(any(Vacante.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vacanteHabilidadRepository.findByVacante_IdVacante(10)).thenReturn(List.of());

        VacanteResponse resultado = vacanteService.cerrar(10);

        assertThat(resultado.getEstado()).isEqualTo("CERRADA");
    }

    @Test
    @DisplayName("agregarHabilidadRequerida duplicada para la misma vacante lanza excepción")
    void agregarHabilidadRequerida_duplicada_lanzaExcepcion() {
        Vacante vacante = new Vacante();
        vacante.setIdVacante(10);

        Habilidad java = new Habilidad();
        java.setIdHabilidad(1);
        java.setNombre("Java");

        VacanteHabilidadRequest request = new VacanteHabilidadRequest();
        request.setIdHabilidad(1);

        when(vacanteRepository.findById(10)).thenReturn(Optional.of(vacante));
        when(habilidadRepository.findById(1)).thenReturn(Optional.of(java));
        when(vacanteHabilidadRepository.existsByVacante_IdVacanteAndHabilidad_IdHabilidad(10, 1))
                .thenReturn(true);

        assertThatThrownBy(() -> vacanteService.agregarHabilidadRequerida(10, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya está registrada");

        verify(vacanteHabilidadRepository, never()).save(any());
    }

    @Test
    @DisplayName("agregarHabilidadRequerida exitosa guarda la relación vacante-habilidad")
    void agregarHabilidadRequerida_flujoExitoso_guardaLaRelacion() {
        Vacante vacante = new Vacante();
        vacante.setIdVacante(10);

        Habilidad java = new Habilidad();
        java.setIdHabilidad(1);
        java.setNombre("Java");

        VacanteHabilidadRequest request = new VacanteHabilidadRequest();
        request.setIdHabilidad(1);
        request.setNivelRequerido("AVANZADO");
        request.setEsObligatoria(true);

        when(vacanteRepository.findById(10)).thenReturn(Optional.of(vacante));
        when(habilidadRepository.findById(1)).thenReturn(Optional.of(java));
        when(vacanteHabilidadRepository.existsByVacante_IdVacanteAndHabilidad_IdHabilidad(10, 1))
                .thenReturn(false);
        when(vacanteHabilidadRepository.save(any(VacanteHabilidad.class))).thenAnswer(inv -> inv.getArgument(0));

        VacanteHabilidadResponse resultado = vacanteService.agregarHabilidadRequerida(10, request);

        assertThat(resultado.getNombreHabilidad()).isEqualTo("Java");
        assertThat(resultado.getNivelRequerido()).isEqualTo("AVANZADO");
        assertThat(resultado.getEsObligatoria()).isTrue();
    }
}
