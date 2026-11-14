// Pruebas E2E del registro publico de candidatos (RF01).
// El correo se genera distinto en cada corrida (con el timestamp de
// Cypress) para que la prueba se pueda correr las veces que sea sin
// chocar con "ya existe una cuenta con ese correo" de una corrida anterior.

describe('Registro de candidato', () => {
  function correoUnico(): string {
    return `cypress.${Date.now()}@correo.com`;
  }

  it('crea una cuenta con datos validos, muestra el mensaje de exito y redirige a /login', () => {
    cy.visit('/registro');
    cy.get('#nombre').type('Cypress');
    cy.get('#apellido').type('Pruebas');
    cy.get('#correo').type(correoUnico());
    cy.get('#contrasena').type('claveDePrueba123');
    cy.get('.btn.btn-primario.btn-full').click();

    cy.contains('.alert-exito', 'Cuenta creada correctamente').should('be.visible');

    // El propio componente redirige solo despues de 1.5s (setTimeout) --
    // le damos margen a Cypress con un timeout mayor al default.
    cy.url({ timeout: 5000 }).should('include', '/login');
  });

  it('marca la contraseña como invalida si tiene menos de 8 caracteres', () => {
    cy.visit('/registro');
    cy.get('#nombre').type('Cypress');
    cy.get('#apellido').type('Pruebas');
    cy.get('#correo').type(correoUnico());
    cy.get('#contrasena').type('corta');
    cy.get('.btn.btn-primario.btn-full').click();

    cy.contains('.error-msg', 'Debe tener al menos 8 caracteres').should('be.visible');
    cy.url().should('include', '/registro');
  });

  it('marca el correo como invalido si el formato no es un correo', () => {
    cy.visit('/registro');
    cy.get('#nombre').type('Cypress');
    cy.get('#apellido').type('Pruebas');
    cy.get('#correo').type('esto-no-es-un-correo');
    cy.get('#contrasena').type('claveDePrueba123');
    cy.get('.btn.btn-primario.btn-full').click();

    cy.contains('.error-msg', 'Ingresa un correo válido').should('be.visible');
    cy.url().should('include', '/registro');
  });

  it('muestra un error del backend si el correo ya esta registrado', () => {
    const correo = correoUnico();

    // Nos registramos una primera vez...
    cy.visit('/registro');
    cy.get('#nombre').type('Cypress');
    cy.get('#apellido').type('Pruebas');
    cy.get('#correo').type(correo);
    cy.get('#contrasena').type('claveDePrueba123');
    cy.get('.btn.btn-primario.btn-full').click();
    cy.contains('.alert-exito', 'Cuenta creada correctamente').should('be.visible');

    // ...y lo intentamos de nuevo con el mismo correo.
    cy.visit('/registro');
    cy.get('#nombre').type('Cypress');
    cy.get('#apellido').type('Pruebas');
    cy.get('#correo').type(correo);
    cy.get('#contrasena').type('claveDePrueba123');
    cy.get('.btn.btn-primario.btn-full').click();

    cy.get('.alert-error').should('be.visible');
    cy.url().should('include', '/registro');
  });
});
