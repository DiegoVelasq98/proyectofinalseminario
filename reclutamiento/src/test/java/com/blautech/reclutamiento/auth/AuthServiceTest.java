package com.blautech.reclutamiento.auth;

import com.blautech.reclutamiento.auth.dto.LoginRequest;
import com.blautech.reclutamiento.auth.dto.LoginResponse;
import com.blautech.reclutamiento.auth.dto.RegistroRequest;
import com.blautech.reclutamiento.rol.Rol;
import com.blautech.reclutamiento.rol.RolRepository;
import com.blautech.reclutamiento.security.JwtUtil;
import com.blautech.reclutamiento.usuario.Usuario;
import com.blautech.reclutamiento.usuario.UsuarioRepository;
import com.blautech.reclutamiento.usuario.dto.UsuarioResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// ============================================================================
// Pruebas de AuthService: registro (RF01) y login (RF01/RF02). Estas son
// las más sensibles de todo el sistema en cuanto a seguridad, así que se
// prueba explícitamente: que la contraseña SIEMPRE se guarde encriptada,
// que el registro público SIEMPRE asigne el rol CANDIDATO, y que el login
// no filtre si el correo existe o no (mismo mensaje de error en ambos casos).
// ============================================================================
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RolRepository rolRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("registrar con un correo ya existente lanza excepción")
    void registrar_correoDuplicado_lanzaExcepcion() {
        RegistroRequest request = new RegistroRequest();
        request.setCorreo("ana@correo.com");
        request.setContrasena("claveSegura1");

        when(usuarioRepository.existsByCorreo("ana@correo.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.registrar(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe una cuenta");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("registrar exitoso: la contraseña se guarda encriptada y el rol asignado es siempre CANDIDATO")
    void registrar_flujoExitoso_encriptaPasswordYAsignaRolCandidato() {
        RegistroRequest request = new RegistroRequest();
        request.setNombre("Ana");
        request.setApellido("Pérez");
        request.setCorreo("ana@correo.com");
        request.setContrasena("claveSegura1");
        request.setTelefono("50212345678");

        Rol rolCandidato = new Rol();
        rolCandidato.setIdRol(1);
        rolCandidato.setNombre("CANDIDATO");

        when(usuarioRepository.existsByCorreo("ana@correo.com")).thenReturn(false);
        when(rolRepository.findByNombre("CANDIDATO")).thenReturn(Optional.of(rolCandidato));
        when(passwordEncoder.encode("claveSegura1")).thenReturn("$2a$10$hashSimulado");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setIdUsuario(1);
            return u;
        });

        UsuarioResponse resultado = authService.registrar(request);

        assertThat(resultado.getCorreo()).isEqualTo("ana@correo.com");
        assertThat(resultado.getRol()).isEqualTo("CANDIDATO");

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getContrasena()).isEqualTo("$2a$10$hashSimulado");
        assertThat(captor.getValue().getContrasena()).isNotEqualTo("claveSegura1");
        assertThat(captor.getValue().getRol().getNombre()).isEqualTo("CANDIDATO");
    }

    @Test
    @DisplayName("registrar cuando el rol CANDIDATO no existe en BD lanza excepción de configuración")
    void registrar_rolCandidatoNoExiste_lanzaExcepcion() {
        RegistroRequest request = new RegistroRequest();
        request.setCorreo("ana@correo.com");
        request.setContrasena("claveSegura1");

        when(usuarioRepository.existsByCorreo("ana@correo.com")).thenReturn(false);
        when(rolRepository.findByNombre("CANDIDATO")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.registrar(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("rol CANDIDATO no existe");
    }

    @Test
    @DisplayName("login con correo inexistente da el mismo mensaje genérico que una contraseña incorrecta")
    void login_correoInexistente_daMensajeGenerico() {
        LoginRequest request = new LoginRequest();
        request.setCorreo("noexiste@correo.com");
        request.setContrasena("cualquiera");

        when(usuarioRepository.findByCorreo("noexiste@correo.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Correo o contraseña incorrectos");
    }

    @Test
    @DisplayName("login con contraseña incorrecta da el mismo mensaje genérico (no revela si el correo existe)")
    void login_passwordIncorrecta_daMensajeGenerico() {
        Usuario usuario = new Usuario();
        usuario.setCorreo("ana@correo.com");
        usuario.setContrasena("$2a$10$hashGuardado");
        usuario.setEstado("ACTIVO");

        LoginRequest request = new LoginRequest();
        request.setCorreo("ana@correo.com");
        request.setContrasena("claveIncorrecta");

        when(usuarioRepository.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("claveIncorrecta", "$2a$10$hashGuardado")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Correo o contraseña incorrectos");
    }

    @Test
    @DisplayName("login de una cuenta inactiva es rechazado aunque la contraseña sea correcta")
    void login_cuentaInactiva_lanzaExcepcion() {
        Usuario usuario = new Usuario();
        usuario.setCorreo("ana@correo.com");
        usuario.setContrasena("$2a$10$hashGuardado");
        usuario.setEstado("INACTIVO");

        LoginRequest request = new LoginRequest();
        request.setCorreo("ana@correo.com");
        request.setContrasena("claveCorrecta");

        when(usuarioRepository.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("claveCorrecta", "$2a$10$hashGuardado")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("inactiva");

        verify(jwtUtil, never()).generarToken(any(), any(), any());
    }

    @Test
    @DisplayName("login exitoso genera un token y actualiza el último acceso del usuario")
    void login_flujoExitoso_generaTokenYActualizaUltimoAcceso() {
        Rol rol = new Rol();
        rol.setNombre("CANDIDATO");

        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1);
        usuario.setNombre("Ana");
        usuario.setCorreo("ana@correo.com");
        usuario.setContrasena("$2a$10$hashGuardado");
        usuario.setEstado("ACTIVO");
        usuario.setRol(rol);

        LoginRequest request = new LoginRequest();
        request.setCorreo("ana@correo.com");
        request.setContrasena("claveCorrecta");

        when(usuarioRepository.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("claveCorrecta", "$2a$10$hashGuardado")).thenReturn(true);
        when(jwtUtil.generarToken("ana@correo.com", 1, "CANDIDATO")).thenReturn("token-jwt-simulado");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        LoginResponse resultado = authService.login(request);

        assertThat(resultado.getToken()).isEqualTo("token-jwt-simulado");
        assertThat(resultado.getRol()).isEqualTo("CANDIDATO");

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getUltimoAcceso()).isNotNull();
    }

    @Test
    @DisplayName("login con una contraseña temporal pendiente avisa al frontend para forzar el cambio")
    void login_conContrasenaTemporalPendiente_marcaDebeCambiarContrasena() {
        Rol rol = new Rol();
        rol.setNombre("CANDIDATO");

        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1);
        usuario.setNombre("Ana");
        usuario.setCorreo("ana@correo.com");
        usuario.setContrasena("$2a$10$hashGuardado");
        usuario.setEstado("ACTIVO");
        usuario.setRol(rol);
        usuario.setDebeCambiarContrasena(true); // el Administrador le reinició la contraseña

        LoginRequest request = new LoginRequest();
        request.setCorreo("ana@correo.com");
        request.setContrasena("temporalAsignada");

        when(usuarioRepository.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("temporalAsignada", "$2a$10$hashGuardado")).thenReturn(true);
        when(jwtUtil.generarToken("ana@correo.com", 1, "CANDIDATO")).thenReturn("token-jwt-simulado");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        LoginResponse resultado = authService.login(request);

        assertThat(resultado.isDebeCambiarContrasena()).isTrue();
    }
}
