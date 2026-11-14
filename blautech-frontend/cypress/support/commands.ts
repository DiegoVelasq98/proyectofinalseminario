/// <reference types="cypress" />

// Comandos reutilizables para las pruebas E2E de Blautech.

declare global {
  namespace Cypress {
    interface Chainable {
      /**
       * Hace login real por la UI (llena el formulario de /login y da clic
       * en "Ingresar"). Se usa la UI y no una llamada directa a la API
       * porque estas son pruebas de FRONTEND: también validan que el
       * formulario, el guard de rutas y el redireccionamiento por rol
       * funcionen de principio a fin.
       */
      loginUI(correo: string, contrasena: string): Chainable<void>;
    }
  }
}

Cypress.Commands.add('loginUI', (correo: string, contrasena: string) => {
  cy.visit('/login');
  cy.get('#correo').clear().type(correo);
  cy.get('#contrasena').clear().type(contrasena);
  cy.get('.btn-login').click();
});

export {};
