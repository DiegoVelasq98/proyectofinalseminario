package com.blautech.reclutamiento.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

// Habilita las peticiones desde el frontend Angular (que corre en un
// origen distinto: http://localhost:4200) hacia este backend
// (http://localhost:9003). Sin esto, el navegador bloquea las
// peticiones del frontend aunque funcionen perfecto desde Postman
// (Postman no aplica la política de origen cruzado que sí aplica un
// navegador real). Este bean lo activa SecurityConfig con .cors(...).
//
// El origen permitido ahora viene de application.properties
// (cors.allowed-origins), con "http://localhost:4200" como valor por
// defecto para que el desarrollo local (ng serve) siga funcionando
// igual que siempre. En Docker, docker-compose sobreescribe esta
// propiedad con la variable de entorno CORS_ALLOWED_ORIGINS. En
// producción, con Nginx haciendo de reverse proxy (ver nginx.conf del
// frontend), el navegador ni siquiera cruza orígenes -- esto queda
// como respaldo para cuando el frontend se sirve aparte del backend.
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:http://localhost:4200}")
    private String origenesPermitidos;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origenes = Arrays.stream(origenesPermitidos.split(","))
                .map(String::trim)
                .filter(origen -> !origen.isEmpty())
                .toList();

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origenes);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
