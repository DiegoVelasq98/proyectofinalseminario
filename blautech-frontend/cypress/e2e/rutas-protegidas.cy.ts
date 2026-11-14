// Pruebas E2E de los guards de ruta (RF02): sin sesion, cualquier
// pantalla protegida manda a /login; con sesion pero del rol equivocado,
// manda a /no-autorizado. Esto es lo mismo que ya protege @PreAuthorize
// en el backend, pero aqui se prueba la experiencia de UI.

describe('Rutas protegidas', () => {
  beforeEach(() => {
    // Sesion limpia antes de cada prueba: sin esto, una corrida anterior
    // que dejo sesion guardada en localStorage haria fallar estas pruebas.
    cy.clearLocalStorage();
  });

  it('sin sesion, /candidato/vacantes redirige a /login', () => {
    cy.visit('/candidato/vacantes');
    cy.url().should('include', '/login');
  });

  it('sin sesion, /rrhh/vacantes redirige a /login', () => {
    cy.visit('/rrhh/vacantes');
    cy.url().should('include', '/login');
  });

  it('sin sesion, /admin/usuarios redirige a /login', () => {
    cy.visit('/admin/usuarios');
    cy.url().should('include', '/login');
  });

  it('un candidato autenticado que intenta entrar a una pantalla de RRHH cae en /no-autorizado', () => {
    cy.fixture('usuarios').then((usuarios) => {
      cy.loginUI(usuarios.candidato.correo, usuarios.candidato.contrasena);
      cy.url().should('include', '/candidato/vacantes');

      cy.visit('/rrhh/vacantes');
      cy.url().should('include', '/no-autorizado');
    });
  });

  it('un usuario de RRHH autenticado que intenta entrar al panel de ADMIN cae en /no-autorizado', () => {
    cy.fixture('usuarios').then((usuarios) => {
      cy.loginUI(usuarios.rrhh.correo, usuarios.rrhh.contrasena);
      cy.url().should('include', '/rrhh/vacantes');

      cy.visit('/admin/usuarios');
      cy.url().should('include', '/no-autorizado');
    });
  });
});
