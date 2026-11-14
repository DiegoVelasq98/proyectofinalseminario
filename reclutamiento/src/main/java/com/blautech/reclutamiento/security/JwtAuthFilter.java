package com.blautech.reclutamiento.security;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// Un "filtro" en Spring es código que se ejecuta ANTES de que la
// petición llegue a tu Controller. OncePerRequestFilter garantiza que
// se ejecute una sola vez por cada petición HTTP que entra.
//
// Este filtro hace un trabajo muy puntual en cada petición que llega:
// 1. Busca el header "Authorization: Bearer <token>".
// 2. Si existe y es válido, le dice a Spring Security "esta petición
//    SÍ está autenticada, y esta es la persona (correo) y su rol".
// 3. Si no hay token o no es válido, simplemente deja pasar la
//    petición sin marcarla como autenticada -- será SecurityConfig
//    quien decida si esa ruta requiere estar autenticado o no.
@Component
@RequiredArgsConstructor // Lombok genera un constructor con los campos "final" (inyecta JwtUtil automáticamente)
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7); // quita el prefijo "Bearer "

            if (jwtUtil.esValido(token)) {
                String correo = jwtUtil.obtenerCorreo(token);
                String rol = jwtUtil.obtenerRol(token);

                // Spring Security espera los roles con el prefijo "ROLE_"
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + rol));

                var authentication = new UsernamePasswordAuthenticationToken(correo, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        // Sea que haya token o no, la petición sigue su curso. Si la ruta
        // requiere autenticación y no se marcó como autenticada arriba,
        // Spring Security la va a rechazar más adelante (eso lo configura
        // SecurityConfig).
        filterChain.doFilter(request, response);
    }
}