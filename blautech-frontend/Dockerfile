# ---------- Etapa 1: compilar con Node (no necesitas Node instalado fuera de Docker) ----------
FROM node:20-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build -- --configuration production

# ---------- Etapa 2: servir los archivos estáticos con Nginx ----------
FROM nginx:1.27-alpine

# El builder "application" de Angular 18 siempre genera la salida final
# dentro de dist/<nombre-proyecto>/browser (con o sin SSR).
COPY --from=build /app/dist/blautech-frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
EXPOSE 443
