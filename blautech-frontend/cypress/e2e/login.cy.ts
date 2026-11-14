// Pruebas E2E del login (RF01/RF02): los 3 roles reales del sistema,
// el mensaje de error con credenciales incorrectas, y la validacion del
// formulario vacio. Usa las credenciales de cypress/fixtures/usuarios.json
// (las del seed_usuarios_prueba.sql del backend).

describe('Login', () => {
  let usuarios: any;

  before(() => {
    cy.fixture('usuarios').then((data) => {
      usuarios = data;
    });
  });

  it('RRHH inicia sesion y es redirigido a /rrhh/vacantes', () => {
    cy.loginUI(usuarios.rrhh.correo, usuarios.rrhh.contrasena);
    cy.url().should('include', '/rrhh/vacantes');
    cy.contains('button', 'Cerrar sesión').should('be.visible');
  });

  it('ADMIN inicia sesion y es redirigido a /admin/usuarios', () => {
    cy.loginUI(usuarios.admin.correo, usuarios.admin.contrasena);
    cy.url().should('include', '/admin/usuarios');
  });

  it('CANDIDATO inicia sesion y es redirigido a /candidato/vacantes', () => {
    cy.loginUI(usuarios.candidato.correo, usuarios.candidato.contrasena);
    cy.url().should('include', '/candidato/vacantes');
  });

  it('muestra un error con credenciales incorrectas y se queda en /login', () => {
    cy.loginUI(usuarios.rrhh.correo, 'unaClaveIncorrecta123');
    cy.url().should('include', '/login');
    cy.get('.alert-error').should('be.visible').and('contain.text', 'incorrect');
  });

  it('no deja enviar el formulario vacio y muestra los mensajes de validacion', () => {
    cy.visit('/login');
    cy.get('.btn-login').click();
    cy.contains('.error-msg', 'Ingresa un correo válido').should('be.visible');
    cy.contains('.error-msg', 'La contraseña es obligatoria').should('be.visible');
    // No debio navegar a ningun lado -- el submit se corta en el propio
    // componente (form.invalid) antes de llamar al backend.
    cy.url().should('include', '/login');
  });

  it('el boton "Cerrar sesión" termina la sesion y regresa al login', () => {
    cy.loginUI(usuarios.candidato.correo, usuarios.candidato.contrasena);
    cy.url().should('include', '/candidato/vacantes');
    cy.contains('button', 'Cerrar sesión').click();
    cy.url().should('include', '/login');
  });
});
