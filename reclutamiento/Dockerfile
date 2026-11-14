# ---------- Etapa 1: compilar con Maven + JDK 21 ----------
# Esta etapa SOLO existe dentro de la imagen de Docker; no necesitas
# tener Maven ni JDK 21 instalados en la máquina donde corras esto.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copiamos primero el pom.xml y descargamos dependencias por separado:
# así Docker puede reusar esta capa en builds futuros si el pom.xml
# no cambió, aunque cambie el código (builds más rápidos).
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Etapa 2: imagen final, solo con el JRE (más liviana) ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Carpeta donde CandidatoService guarda los currículums subidos
# (blautech.curriculums.directorio=archivos/curriculums). La montamos
# como volumen en docker-compose.yml para que los archivos sobrevivan
# aunque se reconstruya el contenedor.
RUN mkdir -p /app/archivos/curriculums

COPY --from=build /app/target/*.jar app.jar

EXPOSE 9003
ENTRYPOINT ["java", "-jar", "app.jar"]
