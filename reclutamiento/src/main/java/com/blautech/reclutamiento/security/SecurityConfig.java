package com.blautech.reclutamiento.security;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// Aquí se define, en un solo lugar, TODA la política de seguridad de
// este microservicio: qué rutas son públicas, cuáles requieren estar
// logueado, y cómo verificamos ese login (con nuestro JwtAuthFilter).
@Configuration
@EnableWebSecurity
// Esto es lo que activa la anotación @PreAuthorize("hasRole('RRHH')")
// que vamos a usar en VacanteController (y en los siguientes módulos)
// para que solo cierto rol pueda ejecutar ciertos endpoints.
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    // BCrypt es el algoritmo estándar para "hashear" contraseñas: convierte
    // "1234567890" en algo como "$2a$10$N9qo8uLOickgx2ZMRZoMy...", que no
    // se puede revertir. Cuando alguien hace login, no desencriptamos su
    // contraseña -- volvemos a hashear lo que escribió y comparamos los
    // dos hashes. Este bean lo va a usar UsuarioService más adelante.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Activa el bean CorsConfigurationSource definido en CorsConfig.
                // Sin esta línea, Spring Security ignora ese bean y el navegador
                // sigue bloqueando las peticiones del frontend (CORS).
                .cors(Customizer.withDefaults())

                // CSRF protege formularios tradicionales de un navegador con
                // sesión. Nosotros no usamos sesiones (usamos JWT sin estado),
                // así que no aplica y lo desactivamos.
                .csrf(csrf -> csrf.disable())

                // STATELESS = "no guardes ninguna sesión en el servidor".
                // Cada petición debe traer su propio token JWT; el servidor no
                // recuerda nada de una petición a la siguiente. Esto es clave
                // para que el servicio pueda escalar (tener varias instancias)
                // sin que importe cuál de ellas atienda cada petición.
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // Cualquiera puede intentar iniciar sesión o registrarse
                        // sin tener un token todavía (¡obviamente, si ya lo
                        // tuvieran no necesitarían hacer login!).
                        // El navegador manda un OPTIONS de "preflight" antes de cada
                        // petición real (POST/PUT/DELETE) entre distintos orígenes.
                        // Ese OPTIONS nunca trae token, así que debe quedar público.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/auth/**").permitAll()
                        // Todo lo demás SÍ requiere un token válido.
                        .anyRequest().authenticated()
                )

                // Inserta nuestro filtro justo antes del filtro estándar de
                // Spring Security, para que sea el primero en revisar el token.
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
