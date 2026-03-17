/// <reference types="cypress" />

const API_URL = "http://localhost:8080";

// Register a new user via API
Cypress.Commands.add("registerUser", (email: string, password: string, role: "PATIENT" | "DOCTOR") => {
  return cy.request({
    method: "POST",
    url: `${API_URL}/auth/register`,
    body: { email, password, role },
    failOnStatusCode: false,
  });
});

// Login via API and return token
Cypress.Commands.add("loginViaAPI", (email: string, password: string) => {
  return cy.request({
    method: "POST",
    url: `${API_URL}/auth/login`,
    body: { email, password },
    failOnStatusCode: false,
  }).then((response) => {
    if (response.status === 200) {
      return response.body.token;
    }
    return null;
  });
});

// Login via UI
Cypress.Commands.add("loginViaUI", (email: string, password: string) => {
  cy.visit("/login");
  cy.get('input[type="email"]').type(email);
  cy.get('input[type="password"]').type(password);
  cy.get('button[type="submit"]').click();
});

// Complete patient onboarding via API
Cypress.Commands.add("onboardPatient", (token: string, firstName: string, lastName: string) => {
  return cy.request({
    method: "POST",
    url: `${API_URL}/onboarding/patient`,
    headers: { Authorization: `Bearer ${token}` },
    body: {
      firstName,
      lastName,
      dateOfBirth: "1990-01-01",
      gender: "Other",
      conditions: "",
      insuranceNumber: "INS123456",
    },
  });
});

// Complete doctor onboarding via API
Cypress.Commands.add("onboardDoctor", (token: string, firstName: string, lastName: string) => {
  return cy.request({
    method: "POST",
    url: `${API_URL}/onboarding/doctor`,
    headers: { Authorization: `Bearer ${token}` },
    body: {
      firstName,
      lastName,
      specialization: "Cardiology",
      bio: "Test doctor",
      licenseNumber: "LIC123456",
      clinicName: "Test Clinic",
      yearsOfExperience: 5,
      photoUrl: null,
      openingHours: "09:00",
      closingHours: "17:00",
      isAbsent: false,
    },
  });
});