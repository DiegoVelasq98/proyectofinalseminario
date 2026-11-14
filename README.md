# Blautech: sistema de reclutamiento con inteligencia artificial

Blautech es una página web que ayuda a Recursos Humanos a hacer la **primera revisión de los currículums**. La persona candidata se registra, sube su currículum y se postula a una vacante. El sistema compara su perfil con lo que pide la vacante y le muestra a Recursos Humanos un resultado de coincidencia **ALTA, MEDIA o BAJA**. La decisión final siempre la toma una persona.

Es el proyecto de graduación de Diego Velásquez (Universidad Mariano Gálvez, Ingeniería en Sistemas) y se usa también como proyecto final del curso Seminario de Tecnologías.

## Dónde verlo funcionando

| Dónde | Dirección | Para qué |
|---|---|---|
| Producción (servidor en la nube, con Docker) | https://blautech.online | Es la versión que probó Recursos Humanos de Blautech Guatemala |
| Kubernetes (clúster del curso) | https://dvelasquezp4.35.188.164.62.nip.io | Es la versión desplegada para el seminario |

## Qué puede hacer cada persona

- **Candidato:** completa su perfil y sus habilidades, sube su currículum, se postula y ve el historial de sus postulaciones.
- **Recursos Humanos:** crea vacantes y define qué habilidades piden, ve a los postulantes con su clasificación (ALTA, MEDIA o BAJA) y cambia el estado de cada postulación.
- **Administrador:** gestiona usuarios y roles, activa o desactiva cuentas y reinicia contraseñas.

## Con qué está hecho

| Parte | Herramienta | Qué hace |
|---|---|---|
| Pantallas | Angular | Lo que ve la persona en el navegador |
| Backend | Spring Boot (Java 21) | Procesa la información y decide qué puede hacer cada persona |
| Base de datos | MySQL 8 | Guarda usuarios, vacantes, postulaciones y resultados |
| Inteligencia artificial | Google Gemini | Compara las habilidades del candidato con las de la vacante. Si no está disponible, el sistema usa reglas fijas y siempre da un resultado |
| Correos | n8n | Envía el correo de confirmación cuando alguien se postula |
| Ejecución | Docker y Kubernetes | Ponen a funcionar todo el sistema |

## Qué hay en cada carpeta

| Carpeta o archivo | Contenido |
|---|---|
| `reclutamiento/` | El backend (Spring Boot) |
| `blautech-frontend/` | Las pantallas (Angular) |
| `database/` | La estructura de la base de datos, los roles y las migraciones |
| `mysql-init/` | Carpeta donde se coloca el script inicial de la base al usar `docker-compose.yml` |
| `n8n/` | El flujo del correo de confirmación y su guía |
| `k8s/` | Los archivos para ponerlo en Kubernetes y su guía paso a paso |
| `docker-compose.yml` | Para ponerlo a funcionar en un servidor con Docker |
| `scripts/` | Copias de respaldo y restauración de la base de datos |
| `docs/` | Documento final del proyecto (PDF) |
| `LICENSE` | Licencia MIT |

## Cómo probarlo en tu computadora (paso a paso)

Necesitás tener instalado Docker Desktop.

1. Copiá el archivo `.env.example` y ponele el nombre `.env`.
2. Abrí `.env` y llená tus valores: la contraseña de la base de datos, una clave larga y aleatoria para las sesiones (`JWT_SECRET`) y, si querés usar la inteligencia artificial, tu clave de Gemini. Si dejás la clave de Gemini vacía, el sistema igual funciona con las reglas fijas.
3. Copiá los archivos `database/schema.sql` y `database/roles.sql` dentro de la carpeta `mysql-init/`. Así la base de datos nace con sus tablas y sus roles.
4. Desde la carpeta principal, corré:
   ```
   docker compose up -d --build
   ```
5. Esperá un par de minutos a que todo arranque y abrí `http://localhost` en el navegador.
6. Para apagarlo:
   ```
   docker compose down
   ```

El archivo `.env` tiene contraseñas y claves reales, por eso **nunca se sube a git**.

## Cómo ponerlo en Kubernetes

La guía completa, con cada paso explicado, está en [`k8s/README-DEPLOY.md`](k8s/README-DEPLOY.md). En resumen: se publican las imágenes en Docker Hub, se crea el secreto con las claves, se aplican los archivos de la carpeta `k8s/` en orden y, al final, se activa el certificado HTTPS.

## Seguridad

Lo que ya está hecho: inicio de sesión con permisos según el rol, contraseñas cifradas, bloqueo de la cuenta tras 5 intentos fallidos, protección para que una persona no vea datos de otra, lista de sitios autorizados (CORS) y conexión segura (HTTPS) en producción y en Kubernetes.

Lo que todavía está pendiente (está explicado con más detalle en el documento del proyecto):

- Quitar la clave de sesiones que viene escrita por defecto en `application.properties`.
- Que el backend use un usuario de MySQL con permisos limitados, en lugar de `root`.
- Proteger mejor los datos de los currículums que se envían a Gemini (usar el nivel de pago o quitar los datos de contacto).
- Agregar reglas de red y restricciones de seguridad en los contenedores de Kubernetes.

## Licencia

El código se comparte con la licencia MIT (ver el archivo `LICENSE`): se puede usar, copiar y modificar conservando el aviso del autor, y no tiene garantía.

## Autor

Diego Fernando Velásquez Pichilla. Universidad Mariano Gálvez, Facultad de Ingeniería en Sistemas.
