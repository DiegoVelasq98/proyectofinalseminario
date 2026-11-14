package com.blautech.reclutamiento.habilidad;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// ============================================================================
// Pruebas de HabilidadService: el catálogo de habilidades que alimenta
// tanto el perfil del candidato (RF06) como los requisitos de vacante (RF04).
// ============================================================================
@ExtendWith(MockitoExtension.class)
class HabilidadServiceTest {

    @Mock private HabilidadRepository habilidadRepository;

    @InjectMocks
    private HabilidadService habilidadService;

    @Test
    @DisplayName("crear una habilidad con nombre ya existente en el catálogo lanza excepción")
    void crear_nombreDuplicado_lanzaExcepcion() {
        when(habilidadRepository.existsByNombre("Java")).thenReturn(true);

        assertThatThrownBy(() -> habilidadService.crear("Java", "Lenguaje de programación"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe una habilidad");

        verify(habilidadRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear una habilidad nueva la guarda correctamente")
    void crear_flujoExitoso_guardaLaHabilidad() {
        when(habilidadRepository.existsByNombre("Java")).thenReturn(false);
        when(habilidadRepository.save(any(Habilidad.class))).thenAnswer(inv -> {
            Habilidad h = inv.getArgument(0);
            h.setIdHabilidad(1);
            return h;
        });

        Habilidad resultado = habilidadService.crear("Java", "Lenguaje de programación");

        assertThat(resultado.getIdHabilidad()).isEqualTo(1);
        assertThat(resultado.getNombre()).isEqualTo("Java");
    }

    @Test
    @DisplayName("listarTodas devuelve el catálogo completo tal como lo entrega el repositorio")
    void listarTodas_devuelveElCatalogoCompleto() {
        Habilidad java = new Habilidad();
        java.setIdHabilidad(1);
        java.setNombre("Java");

        Habilidad sql = new Habilidad();
        sql.setIdHabilidad(2);
        sql.setNombre("SQL");

        when(habilidadRepository.findAll()).thenReturn(List.of(java, sql));

        List<Habilidad> resultado = habilidadService.listarTodas();

        assertThat(resultado).hasSize(2).extracting(Habilidad::getNombre)
                .containsExactly("Java", "SQL");
    }
}
