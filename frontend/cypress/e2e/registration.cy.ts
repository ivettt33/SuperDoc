/// <reference types="cypress" />
/// <reference path="../../cypress.d.ts" />

describe("Registration E2E Tests", () => {
  const timestamp = Date.now();
  const patientEmail = `patient.${timestamp}@test.com`;
  const doctorEmail = `doctor.${timestamp}@test.com`;
  const password = "TestPassword123!";
  const duplicateEmail = `duplicate.${timestamp}@test.com`;

  beforeEach(() => {
    cy.visit("/register");
  });

  it("should successfully register a new patient", () => {
    cy.get('input[type="email"]').type(patientEmail);
    cy.get('input[type="password"]').first().type(password);
    cy.get('input[type="password"]').last().type(password);
    cy.get('select#role').select("PATIENT");
    cy.get('input[type="checkbox"]').check();

    cy.get('button[type="submit"]').click();

    // Should redirect to login page after successful registration
    cy.url().should("include", "/login");
    cy.contains("Sign in", { timeout: 10000 }).should("be.visible");
  });

  it("should successfully register a new doctor", () => {
    cy.get('input[type="email"]').type(doctorEmail);
    cy.get('input[type="password"]').first().type(password);
    cy.get('input[type="password"]').last().type(password);
    cy.get('select#role').select("DOCTOR");
    cy.get('input[type="checkbox"]').check();

    cy.get('button[type="submit"]').click();

    // Should redirect to login page after successful registration
    cy.url().should("include", "/login");
  });

  it("should prevent registration with duplicate email", () => {
    // First registration
    cy.registerUser(duplicateEmail, password, "PATIENT");

    // Try to register again with same email
    cy.visit("/register");
    cy.get('input[type="email"]').type(duplicateEmail);
    cy.get('input[type="password"]').first().type(password);
    cy.get('input[type="password"]').last().type(password);
    cy.get('select#role').select("PATIENT");
    cy.get('input[type="checkbox"]').check();
    cy.get('button[type="submit"]').click();

    // Should show error message
    cy.get(".error-message", { timeout: 5000 }).should("be.visible");
    cy.get(".error-message").should("contain", "already");
  });

  it("should validate email format", () => {
    cy.get('input[type="email"]').type("invalid-email");
    cy.get('input[type="password"]').first().type(password);
    cy.get('input[type="password"]').last().type(password);
    cy.get('select#role').select("PATIENT");
    cy.get('input[type="checkbox"]').check();

    cy.get('button[type="submit"]').click();

    // HTML5 validation should prevent submission or show error
    cy.get('input[type="email"]:invalid').should("exist");
  });

  it("should validate password confirmation match", () => {
    cy.get('input[type="email"]').type(`test${timestamp}@test.com`);
    cy.get('input[type="password"]').first().type(password);
    cy.get('input[type="password"]').last().type("DifferentPassword123!");
    cy.get('select#role').select("PATIENT");
    cy.get('input[type="checkbox"]').check();

    cy.get('button[type="submit"]').click();

    // Should show validation error
    cy.get(".error-message", { timeout: 5000 }).should("be.visible");
    cy.get(".error-message").should("contain", "match");
  });

  it("should require terms acceptance", () => {
    cy.get('input[type="email"]').type(`test${timestamp}@test.com`);
    cy.get('input[type="password"]').first().type(password);
    cy.get('input[type="password"]').last().type(password);
    cy.get('select#role').select("PATIENT");
    // Don't check terms checkbox

    cy.get('button[type="submit"]').click();

    // Should show HTML5 validation error
    cy.get('input[type="checkbox"]:invalid').should("exist");
  });

  it("should validate minimum password length", () => {
    const shortPassword = "short";
    cy.get('input[type="email"]').type(`test${timestamp}@test.com`);
    cy.get('input[type="password"]').first().type(shortPassword);
    cy.get('input[type="password"]').last().type(shortPassword);
    cy.get('select#role').select("PATIENT");
    cy.get('input[type="checkbox"]').check();

    cy.get('button[type="submit"]').click();

    // Should show validation error
    cy.get(".error-message", { timeout: 5000 }).should("be.visible");
    cy.get(".error-message").should("contain", "7");
  });

  it("should allow navigation to login page", () => {
    cy.contains("Sign in").click();
    cy.url().should("include", "/login");
  });

  it("should complete full registration and login flow for patient", () => {
    const fullFlowEmail = `fullflow.${timestamp}@test.com`;

    // Register
    cy.get('input[type="email"]').type(fullFlowEmail);
    cy.get('input[type="password"]').first().type(password);
    cy.get('input[type="password"]').last().type(password);
    cy.get('select#role').select("PATIENT");
    cy.get('input[type="checkbox"]').check();
    cy.get('button[type="submit"]').click();

    // Should redirect to login
    cy.url().should("include", "/login");

    // Login
    cy.loginViaUI(fullFlowEmail, password);

    // Should redirect to onboarding or dashboard
    cy.url({ timeout: 10000 }).should("satisfy", (url: string) => {
      return url.includes("/onboarding") || url === Cypress.config().baseUrl + "/";
    });
  });

  it("should preserve role selection", () => {
    // Select DOCTOR role
    cy.get('select#role').select("DOCTOR");
    cy.get('select#role').should("have.value", "DOCTOR");

    // Select PATIENT role
    cy.get('select#role').select("PATIENT");
    cy.get('select#role').should("have.value", "PATIENT");
  });
});

