package com.blautech.reclutamiento.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

// Esta clase es la única responsable de crear y de leer tokens JWT.
// Piensa en el JWT como una "pulsera de acceso" de un evento: trae
// impreso quién eres (el correo) y a qué zona puedes entrar (el rol),
// y está sellada de una forma que si alguien intenta alterarla, se nota.
@Component
public class JwtUtil {

    // La "llave secreta" con la que se firma y se valida cada token.
    // Los 3 microservicios (ms-usuarios, ms-reclutamiento, ms-analisis)
    // deben usar EXACTAMENTE la misma llave, porque así ms-reclutamiento
    // y ms-analisis pueden comprobar que el token realmente lo emitió
    // ms-usuarios, sin tener que preguntarle nada por red.
    // Se lee desde application.properties (nunca la escribas "quemada" en el código).
    @Value("${jwt.secret}")
    private String secretKey;

    // Cuánto tiempo dura un token antes de que Angular tenga que pedir
    // uno nuevo (haciendo login otra vez). Aquí: 24 horas, en milisegundos.
    @Value("${jwt.expiracion-ms:86400000}")
    private long expiracionMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    // Genera el token cuando el login es exitoso.
    public String generarToken(String correo, Integer idUsuario, String rol) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + expiracionMs);

        return Jwts.builder()
                .subject(correo)                 // "de quién es este token"
                .claim("idUsuario", idUsuario)    // datos extra que guardamos adentro
                .claim("rol", rol)
                .issuedAt(ahora)
                .expiration(expiracion)
                .signWith(getSigningKey())
                .compact();
    }

    // Lee el correo (el "subject") de un token ya validado.
    public String obtenerCorreo(String token) {
        return parseClaims(token).getSubject();
    }

    public String obtenerRol(String token) {
        return parseClaims(token).get("rol", String.class);
    }

    public Integer obtenerIdUsuario(String token) {
        return parseClaims(token).get("idUsuario", Integer.class);
    }

    // Comprueba que el token: (1) esté firmado con NUESTRA llave secreta
    // -es decir, que de verdad lo hayamos emitido nosotros y no venga
    // falsificado- y (2) que no haya expirado. Si algo no cuadra, la
    // librería jjwt lanza una excepción y devolvemos false.
    public boolean esValido(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
