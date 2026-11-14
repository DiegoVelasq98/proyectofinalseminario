# Cómo poner Blautech en Kubernetes (paso a paso)

Esta guía explica cómo se puso Blautech en el clúster de Kubernetes del curso. El clúster es **compartido** entre varios estudiantes: cada uno tiene su propio espacio, que se llama *namespace*. El de este proyecto es `dvelasquezp4`.

El sistema son tres piezas que trabajan juntas:

```
Navegador  -->  frontend (las pantallas)  -->  backend (procesa la información)  -->  mysql (guarda los datos)
```

Solo las pantallas son visibles desde internet. El backend y la base de datos quedan dentro del clúster.

## Qué hace cada archivo

Los archivos de esta carpeta se aplican en este orden:

| Archivo | Qué contiene | Para qué sirve |
|---|---|---|
| `01-configmap.yaml` | Configuración general | Datos de conexión a la base y los sitios autorizados para usar el sistema |
| `02-secret.example.yaml` | Plantilla de claves secretas | Contraseña de la base, clave de sesiones y clave de Gemini. Es solo un modelo: las claves reales no se suben a git |
| `03-mysql.yaml` | La base de datos y su disco | MySQL con un disco de 2 Gi para que los datos no se pierdan |
| `04-backend.yaml` | El backend y su disco | Procesa la información y guarda los currículums en un disco de 500 Mi |
| `05-frontend.yaml` | Las pantallas y el acceso desde internet | Muestra la página y envía lo que va a `/api` al backend |
| `06-mysql-init-configmap.yaml` | Datos iniciales | Crea las tablas y los roles la primera vez. **Se genera con un script y no se sube a git** |
| `07-tls-emisor.yaml` | Pedido del certificado | Le pide a Let's Encrypt un certificado HTTPS para el sitio |
| `08-ingress-tls.yaml` | Acceso con HTTPS | Es el acceso de `05-frontend.yaml` pero con el certificado activado |

## Lo que necesitás antes de empezar

- La herramienta `kubectl` instalada (en Windows: `winget install -e --id Kubernetes.kubectl`).
- El archivo de acceso al clúster que te dio el profesor, ya configurado.
- Docker Desktop y una cuenta en Docker Hub.
- Git Bash (para correr el script del paso 4).

## Paso 1: comprobar que estás conectado al clúster

```
kubectl get pods -n dvelasquezp4
```

Si no da error, estás conectado. La primera vez la lista puede salir vacía.

## Paso 2: publicar las imágenes

Una imagen es el paquete que trae el sistema listo para ejecutarse. Se suben a Docker Hub para que cualquier computadora del clúster pueda descargarlas.

```
docker login
docker build -t diegovelasquez98/blautech-backend:v1 ./reclutamiento
docker build -t diegovelasquez98/blautech-frontend:v1 ./blautech-frontend
docker push diegovelasquez98/blautech-backend:v1
docker push diegovelasquez98/blautech-frontend:v1
```

Si tu cuenta de Docker se creó con Google, en lugar de la contraseña usá un *token de acceso* (se crea en Docker Hub, en la configuración de la cuenta).

## Paso 3: crear el secreto con las claves

1. Copiá `k8s/02-secret.example.yaml` y ponele el nombre `k8s/02-secret.local.yaml`.
2. Abrí la copia y reemplazá los tres valores por los reales: la contraseña de la base, una clave larga y aleatoria para las sesiones y tu clave de Gemini.
3. Aplicalo:
   ```
   kubectl apply -f k8s/02-secret.local.yaml
   ```

El archivo `02-secret.local.yaml` está en el `.gitignore`, así que no se sube a git.

## Paso 4: preparar los datos iniciales de la base

En Git Bash, desde la carpeta principal del proyecto:

```
bash k8s/generar-init-configmap.sh
```

Esto crea el archivo `06-mysql-init-configmap.yaml`, que le dice a la base cómo crear sus tablas la primera vez.

## Paso 5: poner todo en marcha

Uno por uno, en este orden:

```
kubectl apply -f k8s/01-configmap.yaml
kubectl apply -f k8s/06-mysql-init-configmap.yaml
kubectl apply -f k8s/03-mysql.yaml
kubectl apply -f k8s/04-backend.yaml
kubectl apply -f k8s/05-frontend.yaml
```

## Paso 6: comprobar que funciona

```
kubectl get pods -n dvelasquezp4
```

Tienen que aparecer `mysql`, `backend` y `frontend` con `1/1` y `Running`. El backend puede tardar uno o dos minutos en estar listo. Cuando estén, abrí en el navegador:

http://dvelasquezp4.35.188.164.62.nip.io

Si al iniciar sesión sale el error "Invalid CORS request", la dirección que estás usando no está en la lista de sitios autorizados de `01-configmap.yaml` (`CORS_ALLOWED_ORIGINS`). Agregala, aplicá el archivo otra vez y reiniciá el backend con `kubectl rollout restart deployment/backend -n dvelasquezp4`.

## Paso 7: activar el HTTPS (candado)

1. Pedir el certificado:
   ```
   kubectl apply -f k8s/07-tls-emisor.yaml
   ```
2. Esperar uno o dos minutos y revisar:
   ```
   kubectl get certificate -n dvelasquezp4
   ```
   Cuando la columna `READY` diga `True`, el certificado está listo.
3. Activarlo en el acceso:
   ```
   kubectl apply -f k8s/08-ingress-tls.yaml
   ```
4. Abrí `https://dvelasquezp4.35.188.164.62.nip.io` y comprobá que aparezca el candado.

Nota: el clúster no permite abrir puertos directos (cuota de NodePort en 0). Por eso el archivo `07-tls-emisor.yaml` incluye `serviceType: ClusterIP`. Sin esa línea, el certificado se queda en espera con el error "exceeded quota".

Si algo sale mal con el HTTPS, se vuelve al acceso sin certificado con `kubectl apply -f k8s/05-frontend.yaml`.

## Pruebas que se hicieron

- **Recuperación automática.** Se borra el backend con `kubectl delete pod -n dvelasquezp4 -l app=backend` y Kubernetes crea uno nuevo solo, que vuelve a `1/1 Running`.
- **Actualización gradual.** Con `kubectl rollout restart deployment/backend -n dvelasquezp4` Kubernetes cambia el backend sin apagar el resto del sistema. Se sigue con `kubectl rollout status deployment/backend -n dvelasquezp4`. Si se queda esperando por falta de CPU en la cuota, este comando le dice que apague el viejo antes de crear el nuevo:
  ```
  kubectl patch deployment backend -n dvelasquezp4 --type merge -p "{\"spec\":{\"strategy\":{\"rollingUpdate\":{\"maxSurge\":0,\"maxUnavailable\":1}}}}"
  ```
- **Los datos no se pierden.** La base y los currículums usan discos que sobreviven aunque se reinicien los componentes.
- **Usuarios en la nube.** Todos los usuarios se guardan en la tabla `usuario` de MySQL y solo se accede a ella a través del backend.

## Los límites del clúster

El clúster limita lo que cada estudiante puede usar: 10 pods, 300 m de CPU y 1 Gi de memoria como mínimo reservado, y 15 Gi de almacenamiento. Blautech usa 200 m y 640 Mi, así que cabe con margen.

## Lo que todavía falta mejorar

- Hay una sola copia de cada pieza, así que no hay alta disponibilidad real.
- Faltan reglas de red y restricciones de seguridad en los contenedores.
- El backend usa el usuario `root` de MySQL.
- El resto de los pendientes de seguridad están en el README principal y en el documento del proyecto.

## Cómo quitarlo del clúster

Borrá lo que se creó, uno por uno (este usuario no puede borrar el namespace completo):

```
kubectl delete -f k8s/08-ingress-tls.yaml
kubectl delete -f k8s/07-tls-emisor.yaml
kubectl delete -f k8s/05-frontend.yaml
kubectl delete -f k8s/04-backend.yaml
kubectl delete -f k8s/03-mysql.yaml
kubectl delete -f k8s/06-mysql-init-configmap.yaml
kubectl delete -f k8s/01-configmap.yaml
kubectl delete -f k8s/02-secret.local.yaml
```

Ojo: borrar `03-mysql.yaml` borra también el disco con los datos.
