/// <reference types="cypress" />

declare global {
  namespace Cypress {
    interface Chainable {
      registerUser(email: string, password: string, role: "PATIENT" | "DOCTOR"): Chainable<Cypress.Response<any>>;
      loginViaAPI(email: string, password: string): Chainable<string | null>;
      loginViaUI(email: string, password: string): Chainable<void>;
      onboardPatient(token: string, firstName: string, lastName: string): Chainable<Cypress.Response<any>>;
      onboardDoctor(token: string, firstName: string, lastName: string): Chainable<Cypress.Response<any>>;
    }
  }
}

export {};

