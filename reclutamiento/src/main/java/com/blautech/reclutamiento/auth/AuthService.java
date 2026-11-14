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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

// Aquí vive la LÓGICA real del login y el registro. El Controller (que
// viene después) solo va a recibir la petición HTTP y delegarle el
// trabajo a esta clase -- el Controller no debería tomar decisiones,
// solo "traducir" HTTP <-> llamada a Service.
@Service
@RequiredArgsConstructor // Lombok inyecta automáticamente los 4 campos "final" de abajo
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder; // el Bean BCrypt que definimos en SecurityConfig
    private final JwtUtil jwtUtil;

    // Protección contra fuerza bruta en el login: contador de intentos fallidos
    // EN MEMORIA (no se guarda en la BD; se reinicia si el backend se reinicia).
    private static final int MAX_INTENTOS_FALLIDOS = 5;
    private static final Duration DURACION_BLOQUEO = Duration.ofMinutes(5);
    private final ConcurrentHashMap<String, IntentosLogin> intentosPorCorreo = new ConcurrentHashMap<>();

    private static class IntentosLogin {
        int fallos = 0;
        LocalDateTime bloqueadoHasta = null;
    }

    // ------------------------------------------------------------------
    // REGISTRO (candidato se crea su cuenta)
    // ------------------------------------------------------------------
    public UsuarioResponse registrar(RegistroRequest request) {

        // 1. Regla de negocio: no permitir correos repetidos.
        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new IllegalArgumentException("Ya existe una cuenta registrada con ese correo");
        }

        // 2. Buscamos el rol CANDIDATO (recuerda: el registro público
        //    SIEMPRE crea candidatos, nunca RRHH/ADMIN).
        Rol rolCandidato = rolRepository.findByNombre("CANDIDATO")
                .orElseThrow(() -> new IllegalStateException(
                        "El rol CANDIDATO no existe en la base de datos. Revisa el script SQL (seed de roles)."));

        // 3. Armamos la entidad Usuario a partir del DTO que llegó.
        Usuario usuario = new Usuario();
        usuario.setNombre(request.getNombre());
        usuario.setApellido(request.getApellido());
        usuario.setCorreo(request.getCorreo());
        // NUNCA guardamos la contraseña tal cual -- la pasamos por BCrypt.
        usuario.setContrasena(passwordEncoder.encode(request.getContrasena()));
        usuario.setTelefono(request.getTelefono());
        usuario.setRol(rolCandidato);

        Usuario guardado = usuarioRepository.save(usuario);

        // 4. Devolvemos el DTO de respuesta (sin contraseña).
        return new UsuarioResponse(guardado);
    }

    // ------------------------------------------------------------------
    // LOGIN
    // ------------------------------------------------------------------
    public LoginResponse login(LoginRequest request) {

        // 0. Protección contra fuerza bruta: si este correo ya superó el
        //    límite de intentos fallidos, se bloquea temporalmente antes
        //    de siquiera consultar la BD.
        String claveIntentos = request.getCorreo() == null ? "" : request.getCorreo().trim().toLowerCase();
        IntentosLogin intentos = intentosPorCorreo.computeIfAbsent(claveIntentos, k -> new IntentosLogin());

        synchronized (intentos) {
            if (intentos.bloqueadoHasta != null) {
                if (LocalDateTime.now().isBefore(intentos.bloqueadoHasta)) {
                    throw new IllegalStateException(
                            "Demasiados intentos fallidos. Intenta de nuevo en unos minutos.");
                }
                // Ya pasó el tiempo de bloqueo: se reinicia el conteo.
                intentos.bloqueadoHasta = null;
                intentos.fallos = 0;
            }
        }

        // 1. Buscamos al usuario por correo. Si no existe, no decimos
        //    "el correo no existe" (eso ayuda a un atacante a adivinar
        //    qué correos SÍ están registrados) -- devolvemos el mismo
        //    mensaje genérico que si la contraseña estuviera mal.
        Usuario usuario = usuarioRepository.findByCorreo(request.getCorreo()).orElse(null);

        // 2. Comparamos la contraseña escrita contra el hash guardado.
        //    matches() internamente vuelve a hashear lo escrito y compara
        //    los hashes -- nunca se desencripta nada, porque BCrypt NO
        //    es reversible.
        boolean credencialesValidas = usuario != null
                && passwordEncoder.matches(request.getContrasena(), usuario.getContrasena());

        if (!credencialesValidas) {
            synchronized (intentos) {
                intentos.fallos++;
                if (intentos.fallos >= MAX_INTENTOS_FALLIDOS) {
                    intentos.bloqueadoHasta = LocalDateTime.now().plus(DURACION_BLOQUEO);
                }
            }
            throw new IllegalArgumentException("Correo o contraseña incorrectos");
        }

        // 3. Regla de negocio: no dejar entrar a cuentas inactivas.
        if (!"ACTIVO".equals(usuario.getEstado())) {
            throw new IllegalStateException("Esta cuenta se encuentra inactiva");
        }

        // Login exitoso: se limpia cualquier registro de intentos fallidos previos.
        intentosPorCorreo.remove(claveIntentos);

        // 4. Actualizamos el último acceso (útil para auditoría).
        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(usuario);

        // 5. Generamos el token con los datos clave del usuario adentro.
        String token = jwtUtil.generarToken(
                usuario.getCorreo(),
                usuario.getIdUsuario(),
                usuario.getRol().getNombre()
        );

        return new LoginResponse(
                token,
                usuario.getIdUsuario(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getRol().getNombre(),
                Boolean.TRUE.equals(usuario.getDebeCambiarContrasena())
        );
    }
}
