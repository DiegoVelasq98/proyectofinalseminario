package com.blautech.reclutamiento.usuario;

import com.blautech.reclutamiento.rol.Rol;
import com.blautech.reclutamiento.rol.RolRepository;
import com.blautech.reclutamiento.usuario.dto.CambiarContrasenaRequest;
import com.blautech.reclutamiento.usuario.dto.CambiarEstadoUsuarioRequest;
import com.blautech.reclutamiento.usuario.dto.CambiarRolRequest;
import com.blautech.reclutamiento.usuario.dto.ReiniciarContrasenaResponse;
import com.blautech.reclutamiento.usuario.dto.UsuarioResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

// Soporta CU05 (Gestionar usuarios y accesos), agregado al Capítulo III
// junto con RF16. Sigue el mismo patrón Service que VacanteService y
// PostulacionService: valida, aplica el cambio sobre la entidad y
// devuelve el DTO de respuesta -- nunca la entidad completa (para no
// filtrar el hash de la contraseña).
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder; // el mismo Bean BCrypt que usa AuthService para el registro/login

    // Alfabeto sin caracteres que se confunden fácilmente al leerlos en
    // pantalla o dictarlos por teléfono (se excluyen 0/O, 1/l/I).
    private static final String ALFABETO_CONTRASENA_TEMPORAL =
            "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final int LONGITUD_CONTRASENA_TEMPORAL = 10;
    private final SecureRandom random = new SecureRandom();

    public List<UsuarioResponse> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioResponse::new)
                .collect(Collectors.toList());
    }

    public UsuarioResponse cambiarRol(Integer idUsuario, CambiarRolRequest request) {
        Usuario usuario = buscarPorId(idUsuario);

        Rol nuevoRol = rolRepository.findByNombre(request.getNombreRol())
                .orElseThrow(() -> new IllegalArgumentException(
                        "El rol '" + request.getNombreRol() + "' no existe"));

        usuario.setRol(nuevoRol);
        return new UsuarioResponse(usuarioRepository.save(usuario));
    }

    public UsuarioResponse cambiarEstado(Integer idUsuario, CambiarEstadoUsuarioRequest request) {
        Usuario usuario = buscarPorId(idUsuario);

        if (!request.getEstado().equals("ACTIVO") && !request.getEstado().equals("INACTIVO")) {
            throw new IllegalArgumentException("El estado debe ser ACTIVO o INACTIVO");
        }

        usuario.setEstado(request.getEstado());
        return new UsuarioResponse(usuarioRepository.save(usuario));
    }

    // PUT /usuarios/{id}/contrasena/reiniciar -> reemplaza al enlace
    // "¿Olvidaste tu contraseña?" que había en el login. Como el proyecto
    // no tiene configurado el envío de correos (sin SMTP, ver
    // application.properties), el reinicio no puede ser self-service por
    // correo: lo dispara el Administrador desde "Usuario y Acceso" y la
    // contraseña temporal se le muestra UNA sola vez en pantalla para que
    // se la entregue al usuario por el canal que use con él (p. ej.
    // WhatsApp o en persona). Nunca se guarda en texto plano ni se vuelve
    // a poder consultar después de esta respuesta.
    public ReiniciarContrasenaResponse reiniciarContrasena(Integer idUsuario) {
        Usuario usuario = buscarPorId(idUsuario);

        String contrasenaTemporal = generarContrasenaTemporal();
        usuario.setContrasena(passwordEncoder.encode(contrasenaTemporal));
        // El usuario tendrá que reemplazarla antes de poder usar el
        // resto del sistema (ver cambiarContrasenaPropia más abajo).
        usuario.setDebeCambiarContrasena(true);
        usuarioRepository.save(usuario);

        return new ReiniciarContrasenaResponse(
                usuario.getIdUsuario(), usuario.getCorreo(), contrasenaTemporal);
    }

    // PUT /mi-cuenta/contrasena -> el propio usuario logueado cambia su
    // contraseña (típicamente, justo después de entrar con la temporal
    // que le generó el Administrador). A diferencia de reiniciarContrasena,
    // aquí SÍ hay que demostrar que se conoce la contraseña vigente.
    public void cambiarContrasenaPropia(String correo, CambiarContrasenaRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new IllegalArgumentException("El usuario indicado no existe"));

        if (!passwordEncoder.matches(request.getContrasenaActual(), usuario.getContrasena())) {
            throw new IllegalArgumentException("La contraseña actual no es correcta");
        }

        usuario.setContrasena(passwordEncoder.encode(request.getContrasenaNueva()));
        usuario.setDebeCambiarContrasena(false);
        usuarioRepository.save(usuario);
    }

    private String generarContrasenaTemporal() {
        StringBuilder sb = new StringBuilder(LONGITUD_CONTRASENA_TEMPORAL);
        for (int i = 0; i < LONGITUD_CONTRASENA_TEMPORAL; i++) {
            sb.append(ALFABETO_CONTRASENA_TEMPORAL.charAt(random.nextInt(ALFABETO_CONTRASENA_TEMPORAL.length())));
        }
        return sb.toString();
    }

    private Usuario buscarPorId(Integer idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("El usuario indicado no existe"));
    }
}
