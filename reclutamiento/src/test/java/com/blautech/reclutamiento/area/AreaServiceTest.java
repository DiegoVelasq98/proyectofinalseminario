package com.blautech.reclutamiento.area;

import com.blautech.reclutamiento.area.dto.AreaRequest;
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
// Pruebas de AreaService: el catálogo de áreas/departamentos a los que
// puede pertenecer una vacante. Sigue el mismo patrón "estado" (ACTIVO/
// INACTIVO) que ya prueban UsuarioServiceTest y VacanteServiceTest, en vez
// de un DELETE físico.
// ============================================================================
@ExtendWith(MockitoExtension.class)
class AreaServiceTest {

    @Mock private AreaRepository areaRepository;

    @InjectMocks
    private AreaService areaService;

    private Area areaExistente(Integer id, String nombre, String estado) {
        Area area = new Area();
        area.setIdArea(id);
        area.setNombre(nombre);
        area.setEstado(estado);
        return area;
    }

    private AreaRequest request(String nombre) {
        AreaRequest request = new AreaRequest();
        request.setNombre(nombre);
        return request;
    }

    @Test
    @DisplayName("listarActivas devuelve solo las áreas en estado ACTIVO")
    void listarActivas_devuelveSoloLasActivas() {
        Area tecnologia = areaExistente(1, "Tecnología", "ACTIVO");

        when(areaRepository.findByEstado("ACTIVO")).thenReturn(List.of(tecnologia));

        List<Area> resultado = areaService.listarActivas();

        assertThat(resultado).hasSize(1).extracting(Area::getNombre).containsExactly("Tecnología");
    }

    @Test
    @DisplayName("listarTodas devuelve el catálogo completo, incluyendo inactivas")
    void listarTodas_devuelveElCatalogoCompleto() {
        Area tecnologia = areaExistente(1, "Tecnología", "ACTIVO");
        Area calidad = areaExistente(2, "Calidad", "INACTIVO");

        when(areaRepository.findAll()).thenReturn(List.of(tecnologia, calidad));

        List<Area> resultado = areaService.listarTodas();

        assertThat(resultado).hasSize(2).extracting(Area::getNombre)
                .containsExactly("Tecnología", "Calidad");
    }

    @Test
    @DisplayName("crear un área con un nombre ya existente en el catálogo lanza excepción")
    void crear_nombreDuplicado_lanzaExcepcion() {
        when(areaRepository.existsByNombre("Tecnología")).thenReturn(true);

        assertThatThrownBy(() -> areaService.crear(request("Tecnología")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe un área");

        verify(areaRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear un área nueva la guarda correctamente")
    void crear_flujoExitoso_guardaElArea() {
        when(areaRepository.existsByNombre("Infraestructura")).thenReturn(false);
        when(areaRepository.save(any(Area.class))).thenAnswer(inv -> {
            Area area = inv.getArgument(0);
            area.setIdArea(3);
            return area;
        });

        Area resultado = areaService.crear(request("Infraestructura"));

        assertThat(resultado.getIdArea()).isEqualTo(3);
        assertThat(resultado.getNombre()).isEqualTo("Infraestructura");
        assertThat(resultado.getEstado()).isEqualTo("ACTIVO");
    }

    @Test
    @DisplayName("actualizar un área con un ID que no existe lanza excepción")
    void actualizar_idInexistente_lanzaExcepcion() {
        when(areaRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> areaService.actualizar(99, request("Datos")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El área indicada no existe");

        verify(areaRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizar un área cambiando el nombre a uno ya usado por otra área lanza excepción")
    void actualizar_nombreYaUsadoPorOtraArea_lanzaExcepcion() {
        Area tecnologia = areaExistente(1, "Tecnología", "ACTIVO");

        when(areaRepository.findById(1)).thenReturn(Optional.of(tecnologia));
        when(areaRepository.existsByNombre("Calidad")).thenReturn(true);

        assertThatThrownBy(() -> areaService.actualizar(1, request("Calidad")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe un área");

        verify(areaRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizar un área dejando el mismo nombre no valida duplicado y la guarda")
    void actualizar_mismoNombre_noValidaDuplicadoYGuarda() {
        Area tecnologia = areaExistente(1, "Tecnología", "ACTIVO");

        when(areaRepository.findById(1)).thenReturn(Optional.of(tecnologia));
        when(areaRepository.save(any(Area.class))).thenAnswer(inv -> inv.getArgument(0));

        Area resultado = areaService.actualizar(1, request("Tecnología"));

        assertThat(resultado.getNombre()).isEqualTo("Tecnología");
        verify(areaRepository, never()).existsByNombre(any());
    }

    @Test
    @DisplayName("desactivar un área existente cambia su estado a INACTIVO")
    void desactivar_cambiaElEstadoAInactivo() {
        Area tecnologia = areaExistente(1, "Tecnología", "ACTIVO");

        when(areaRepository.findById(1)).thenReturn(Optional.of(tecnologia));
        when(areaRepository.save(any(Area.class))).thenAnswer(inv -> inv.getArgument(0));

        Area resultado = areaService.desactivar(1);

        assertThat(resultado.getEstado()).isEqualTo("INACTIVO");
    }

    @Test
    @DisplayName("activar un área existente cambia su estado a ACTIVO")
    void activar_cambiaElEstadoAActivo() {
        Area calidad = areaExistente(2, "Calidad", "INACTIVO");

        when(areaRepository.findById(2)).thenReturn(Optional.of(calidad));
        when(areaRepository.save(any(Area.class))).thenAnswer(inv -> inv.getArgument(0));

        Area resultado = areaService.activar(2);

        assertThat(resultado.getEstado()).isEqualTo("ACTIVO");
    }

    @Test
    @DisplayName("desactivar un área con un ID que no existe lanza excepción")
    void desactivar_idInexistente_lanzaExcepcion() {
        when(areaRepository.findById(50)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> areaService.desactivar(50))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El área indicada no existe");
    }
}
