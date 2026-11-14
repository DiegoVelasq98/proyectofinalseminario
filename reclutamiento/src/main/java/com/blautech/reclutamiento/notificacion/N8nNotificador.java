package com.blautech.reclutamiento.notificacion;

import com.blautech.reclutamiento.postulacion.Postulacion;
import com.blautech.reclutamiento.usuario.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.format.DateTimeFormatter;

// ============================================================================
// Avisa a n8n cuando un candidato se postula, para que n8n le envíe un correo
// de confirmación al candidato.
//
// DISEÑO PENSADO PARA NO AFECTAR NADA:
//  - Viene APAGADO por defecto (n8n.habilitado=false): si no se activa, este
//    componente no hace absolutamente nada.
//  - La llamada es asíncrona (no hace esperar al candidato) y con tiempos
//    límite cortos.
//  - Cualquier error (n8n caído, red, datos nulos) se captura y solo deja un
//    aviso en el log: JAMÁS se propaga al flujo de postulación.
//  - n8n corre en la red interna de Docker y no se expone a internet.
// ============================================================================
@Component
public class N8nNotificador {

    private static final Logger log = LoggerFactory.getLogger(N8nNotificador.class);

    private final boolean habilitado;
    private final String webhookUrlBase;
    // HTTP/1.1 a propósito: por defecto el cliente de Java intenta negociar
    // HTTP/2 (cabecera "Upgrade") y n8n deja esa petición sin respuesta,
    // lo que termina en un timeout aunque n8n esté funcionando bien.
    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    public N8nNotificador(
            @Value("${n8n.habilitado:false}") boolean habilitado,
            @Value("${n8n.webhook-url:}") String webhookUrlBase) {
        this.habilitado = habilitado;
        this.webhookUrlBase = webhookUrlBase == null ? "" : webhookUrlBase.trim();
    }

    /** Evento: un candidato acaba de postularse a una vacante. */
    public void postulacionCreada(Postulacion postulacion) {
        if (!habilitado || webhookUrlBase.isBlank()) {
            return;
        }
        try {
            Usuario candidato = postulacion.getCandidato().getUsuario();

            // Todo se arma como texto AQUÍ (dentro de la petición HTTP original,
            // donde las relaciones lazy de JPA siguen disponibles); el hilo
            // asíncrono ya solo envía el texto.
            String json = "{"
                    + campo("candidatoNombre", nombreCompleto(candidato)) + ","
                    + campo("candidatoCorreo", candidato.getCorreo()) + ","
                    + campo("vacanteTitulo", postulacion.getVacante().getTitulo()) + ","
                    + campo("fechaPostulacion", postulacion.getFechaPostulacion() == null ? ""
                        : postulacion.getFechaPostulacion().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                    + "}";

            String base = webhookUrlBase.endsWith("/") ? webhookUrlBase : webhookUrlBase + "/";
            HttpRequest request = HttpRequest.newBuilder(URI.create(base + "postulacion-creada"))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            http.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .whenComplete((resp, error) -> {
                        if (error != null) {
                            log.warn("n8n no respondió (la postulación NO se afecta): {}", error.toString());
                        } else if (resp.statusCode() >= 300) {
                            log.warn("n8n respondió HTTP {} al aviso de postulación", resp.statusCode());
                        } else {
                            log.info("Aviso de postulación enviado a n8n correctamente");
                        }
                    });
        } catch (Exception e) {
            log.warn("No se pudo preparar el aviso a n8n (la postulación NO se afecta): {}", e.toString());
        }
    }

    private static String nombreCompleto(Usuario u) {
        if (u == null) return "";
        String n = u.getNombre() == null ? "" : u.getNombre();
        String a = u.getApellido() == null ? "" : u.getApellido();
        return (n + " " + a).trim();
    }

    private static String campo(String nombre, String valor) {
        return "\"" + nombre + "\":\"" + escapar(valor) + "\"";
    }

    // Escape mínimo de JSON para texto libre (nombres, títulos de vacante).
    private static String escapar(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.toString();
    }
}
