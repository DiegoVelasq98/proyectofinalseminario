package com.blautech.reclutamiento.usuario;

import com.blautech.reclutamiento.rol.Rol;
import com.blautech.reclutamiento.rol.RolRepository;
import com.blautech.reclutamiento.usuario.dto.CambiarContrasenaRequest;
import com.blautech.reclutamiento.usuario.dto.CambiarEstadoUsuarioRequest;
import com.blautech.reclutamiento.usuario.dto.CambiarRolRequest;
import com.blautech.reclutamiento.usuario.dto.ReiniciarContrasenaResponse;
import com.blautech.reclutamiento.usuario.dto.UsuarioResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// ============================================================================
// Pruebas de UsuarioService: RF16 / CU05 (módulo de usuarios y accesos,
// exclusivo del rol ADMIN).
// ============================================================================
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RolRepository rolRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        Rol rolActual = new Rol();
        rolActual.setIdRol(1);
        rolActual.setNombre("CANDIDATO");

        usuario = new Usuario();
        usuario.setIdUsuario(1);
        usuario.setNombre("Ana");
        usuario.setApellido("Pérez");
        usuario.setCorreo("ana@correo.com");
        usuario.setRol(rolActual);
        usuario.setEstado("ACTIVO");
    }

    @Test
    @DisplayName("cambiarRol de un usuario que no existe lanza excepción")
    void cambiarRol_usuarioInexistente_lanzaExcepcion() {
        when(usuarioRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.cambiarRol(999, new CambiarRolRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no existe");
    }

    @Test
    @DisplayName("cambiarRol a un rol que no existe en el catálogo lanza excepción")
    void cambiarRol_rolInexistente_lanzaExcepcion() {
        CambiarRolRequest request = new CambiarRolRequest();
        request.setNombreRol("SUPERVISOR");

        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(rolRepository.findByNombre("SUPERVISOR")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.cambiarRol(1, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SUPERVISOR");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("cambiarRol exitoso actualiza el rol asignado al usuario")
    void cambiarRol_flujoExitoso_actualizaRol() {
        Rol rolRrhh = new Rol();
        rolRrhh.setIdRol(2);
        rolRrhh.setNombre("RRHH");

        CambiarRolRequest request = new CambiarRolRequest();
        request.setNombreRol("RRHH");

        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(rolRepository.findByNombre("RRHH")).thenReturn(Optional.of(rolRrhh));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse resultado = usuarioService.cambiarRol(1, request);

        assertThat(resultado.getRol()).isEqualTo("RRHH");
    }

    @Test
    @DisplayName("cambiarEstado con un valor distinto de ACTIVO/INACTIVO lanza excepción")
    void cambiarEstado_valorInvalido_lanzaExcepcion() {
        CambiarEstadoUsuarioRequest request = new CambiarEstadoUsuarioRequest();
        request.setEstado("SUSPENDIDO");

        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> usuarioService.cambiarEstado(1, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ACTIVO o INACTIVO");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("cambiarEstado exitoso desactiva el acceso del usuario")
    void cambiarEstado_flujoExitoso_desactivaAcceso() {
        CambiarEstadoUsuarioRequest request = new CambiarEstadoUsuarioRequest();
        request.setEstado("INACTIVO");

        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse resultado = usuarioService.cambiarEstado(1, request);

        assertThat(resultado.getEstado()).isEqualTo("INACTIVO");
    }

    @Test
    @DisplayName("listarTodos devuelve un DTO por cada usuario, sin exponer la contraseña")
    void listarTodos_devuelveTodosLosUsuariosComoDto() {
        when(usuarioRepository.findAll()).thenReturn(List.of(usuario));

        List<UsuarioResponse> resultado = usuarioService.listarTodos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getCorreo()).isEqualTo("ana@correo.com");
    }

    @Test
    @DisplayName("reiniciarContrasena de un usuario que no existe lanza excepción")
    void reiniciarContrasena_usuarioInexistente_lanzaExcepcion() {
        when(usuarioRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.reiniciarContrasena(999))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no existe");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("reiniciarContrasena genera una contraseña temporal, la hashea y nunca la persiste en texto plano")
    void reiniciarContrasena_flujoExitoso_generaYHasheaContrasenaTemporal() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode(any())).thenReturn("hash-simulado");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        ReiniciarContrasenaResponse resultado = usuarioService.reiniciarContrasena(1);

        assertThat(resultado.getIdUsuario()).isEqualTo(1);
        assertThat(resultado.getCorreo()).isEqualTo("ana@correo.com");
        assertThat(resultado.getContrasenaTemporal()).hasSize(10);
        assertThat(resultado.getContrasenaTemporal()).isNotEqualTo("hash-simulado");

        verify(passwordEncoder).encode(resultado.getContrasenaTemporal());
        assertThat(usuario.getContrasena()).isEqualTo("hash-simulado");
    }

    @Test
    @DisplayName("cambiarContrasenaPropia con la contraseña actual incorrecta lanza excepción")
    void cambiarContrasenaPropia_contrasenaActualIncorrecta_lanzaExcepcion() {
        usuario.setContrasena("$2a$10$hashGuardado");

        CambiarContrasenaRequest request = new CambiarContrasenaRequest();
        request.setContrasenaActual("loQueEscribioMal");
        request.setContrasenaNueva("miNuevaClave123");

        when(usuarioRepository.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("loQueEscribioMal", "$2a$10$hashGuardado")).thenReturn(false);

        assertThatThrownBy(() -> usuarioService.cambiarContrasenaPropia("ana@correo.com", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no es correcta");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("cambiarContrasenaPropia exitoso guarda el hash nuevo y apaga la bandera de cambio obligatorio")
    void cambiarContrasenaPropia_flujoExitoso_actualizaHashYApagaBandera() {
        usuario.setContrasena("$2a$10$hashTemporal");
        usuario.setDebeCambiarContrasena(true);

        CambiarContrasenaRequest request = new CambiarContrasenaRequest();
        request.setContrasenaActual("temporalAsignada");
        request.setContrasenaNueva("miNuevaClave123");

        when(usuarioRepository.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("temporalAsignada", "$2a$10$hashTemporal")).thenReturn(true);
        when(passwordEncoder.encode("miNuevaClave123")).thenReturn("$2a$10$hashNuevo");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        usuarioService.cambiarContrasenaPropia("ana@correo.com", request);

        assertThat(usuario.getContrasena()).isEqualTo("$2a$10$hashNuevo");
        assertThat(usuario.getDebeCambiarContrasena()).isFalse();
    }
}
