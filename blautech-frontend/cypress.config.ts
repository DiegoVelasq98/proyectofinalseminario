import { defineConfig } from 'cypress';

// Config base de Cypress para Blautech (E2E, contra el frontend real
// corriendo en localhost:4200 -- levantar "ng serve" y el backend antes
// de correr las pruebas, Cypress no los levanta por ti).
export default defineConfig({
  e2e: {
    baseUrl: 'http://localhost:4200',
    viewportWidth: 1366,
    viewportHeight: 800,
    video: false,
    defaultCommandTimeout: 8000,
    setupNodeEvents(on, config) {
      // Aquí se registran tareas/plugins de Node cuando hagan falta.
      return config;
    },
  },
});
