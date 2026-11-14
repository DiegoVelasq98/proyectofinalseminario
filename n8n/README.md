# n8n: correo de confirmación al candidato (opcional)

Cuando un candidato se postula, el backend avisa a n8n y n8n le envía un correo de
confirmación a la dirección con la que se registró.

Es **opcional y está apagado por defecto**. Sin activarlo, el sistema funciona igual
que antes. Si n8n está caído, la postulación se guarda igual (el aviso falla en
silencio y solo deja una advertencia en el log).

## Activarlo en el servidor

1. Agregar al `.env` real del servidor: `N8N_HABILITADO=true`
2. Levantar n8n (no arranca con un `up -d` normal, solo con su perfil):
   `docker compose --profile automatizacion up -d n8n`
3. Entrar al editor por túnel SSH (n8n solo escucha en 127.0.0.1 del servidor).
   En tu PC: `ssh -L 5678:localhost:5678 root@IP_DEL_SERVIDOR` y abrir
   `http://localhost:5678` (la primera vez pide crear la cuenta de n8n).
4. Crear la credencial **SMTP** llamada `SMTP Blautech`: para Gmail, host
   `smtp.gmail.com`, puerto `465`, SSL activado, usuario = tu Gmail, contraseña =
   **contraseña de aplicación** de Google.
5. Importar `n8n/flujo-confirmacion-postulacion.json`, abrir el nodo "Correo al
   candidato", elegir la credencial y cambiar `TU_CORREO@gmail.com` por tu Gmail.
6. **Activar** el flujo (interruptor "Active").
7. Reiniciar el backend: `docker compose up -d backend`

## Apagarlo
`N8N_HABILITADO=false` y `docker compose up -d backend`.
