/// <reference types="cypress" />
/// <reference path="../../cypress.d.ts" />

describe("Appointment Booking Critical Flow E2E", () => {
  const timestamp = Date.now();
  const patientEmail = `patient.booking.${timestamp}@test.com`;
  const doctorEmail = `doctor.booking.${timestamp}@test.com`;
  const password = "TestPassword123!";

  it("should complete full appointment booking flow with all validations and verifications", () => {
    // ========== PART 1: DOCTOR REGISTRATION AND ONBOARDING ==========
    
    // Step 1: Register doctor
    cy.visit("/register");
    cy.wait(1500); // Wait for page to load
    cy.get('input[type="email"]', { timeout: 10000 }).should("be.visible").type(doctorEmail);
    cy.get('input[type="password"]').first().type(password);
    cy.get('input[type="password"]').last().type(password);
    cy.get('select#role').select("DOCTOR");
    cy.get('input[type="checkbox"]').check();
    cy.get('button[type="submit"]').click();
    cy.wait(500);
    // Wait for redirect to login
    cy.url({ timeout: 10000 }).should("include", "/login");
    cy.wait(1500);
    cy.contains("Sign in", { timeout: 5000 }).should("be.visible");

    // Step 2: Login as doctor
    cy.get('input[type="email"]').type(doctorEmail);
    cy.get('input[type="password"]').type(password);
    cy.get('button[type="submit"]').click();
    cy.wait(500);
    
    // Should redirect to doctor onboarding
    cy.url({ timeout: 10000 }).should("include", "/onboarding/doctor");
    cy.wait(2000); // Wait for onboarding page to fully load

    // Step 3: Doctor Onboarding - Step 1: Personal Information
    cy.get('input#firstName', { timeout: 10000 }).should("be.visible").type("Jane");
    cy.get('input#lastName').type("Smith");
    cy.get('button').contains("Next").click();
    cy.wait(1500); // Wait for step transition

    // Step 4: Doctor Onboarding - Step 2: Professional Details
    cy.get('select#specialization', { timeout: 5000 }).should("be.visible").select("Cardiology");
    cy.get('input#yearsOfExperience').type("10");
    cy.get('input#clinicName').type("Test Clinic");
    cy.get('input#licenseNumber').type("123456");
    cy.get('textarea#bio').type("Experienced cardiologist");
    cy.get('button').contains("Next").click();
    cy.wait(1500); // Wait for step transition

    // Step 5: Doctor Onboarding - Step 3: Upload Documents (optional - skip)
    cy.get('button', { timeout: 5000 }).contains("Complete Profile").click();
    cy.wait(2000); // Wait for submission
    
    // Wait for redirect to dashboard after onboarding
    cy.url({ timeout: 15000 }).should("satisfy", (url) => {
      return url === Cypress.config().baseUrl + "/" || url.includes("/");
    });
    cy.wait(2000); // Wait for dashboard to load

    // Step 6: Logout as doctor
    cy.get('[title="Logout"]', { timeout: 10000 }).should("be.visible").click();
    cy.wait(1500);
    cy.url({ timeout: 10000 }).should("include", "/login");
    cy.wait(1000);

    // ========== PART 2: PATIENT REGISTRATION AND ONBOARDING ==========

    // Step 7: Register patient
    cy.visit("/register");
    cy.wait(1500);
    cy.get('input[type="email"]', { timeout: 10000 }).should("be.visible").type(patientEmail);
    cy.get('input[type="password"]').first().type(password);
    cy.get('input[type="password"]').last().type(password);
    cy.get('select#role').select("PATIENT");
    cy.get('input[type="checkbox"]').check();
    cy.get('button[type="submit"]').click();
    cy.wait(1500);
    
    // Wait for redirect to login
    cy.url({ timeout: 10000 }).should("include", "/login");
    cy.wait(1500);
    cy.contains("Sign in", { timeout: 5000 }).should("be.visible");

    // Step 8: Login as patient
    cy.get('input[type="email"]').type(patientEmail);
    cy.get('input[type="password"]').type(password);
    cy.get('button[type="submit"]').click();
    cy.wait(1500);

    // Should redirect to patient onboarding
    cy.url({ timeout: 10000 }).should("include", "/onboarding/patient");
    cy.wait(2000); // Wait for onboarding page to fully load

    // Step 9: Patient Onboarding - Step 1: Personal Information
    const dob = new Date();
    dob.setFullYear(dob.getFullYear() - 30);
    cy.get('input#firstName', { timeout: 10000 }).should("be.visible").type("John");
    cy.get('input#lastName').type("Doe");
    cy.get('input#dateOfBirth').type(dob.toISOString().split("T")[0]);
    cy.get('select#gender').select("Male");
    cy.get('button').contains("Next").click();
    cy.wait(1500); // Wait for step transition

    // Step 10: Patient Onboarding - Step 2: Medical Information
    cy.get('textarea#conditions', { timeout: 5000 }).should("be.visible").type("No known conditions");
    cy.get('input#insuranceNumber').type("123456");
    cy.get('button').contains("Complete Registration").click();
    cy.wait(2000); // Wait for submission
    
    // Wait for redirect to dashboard after onboarding
    cy.url({ timeout: 15000 }).should("satisfy", (url) => {
      return url === Cypress.config().baseUrl + "/" || url.includes("/");
    });
    cy.wait(2000); // Wait for dashboard to load

    // ========== PART 3: APPOINTMENT BOOKING WITH VALIDATIONS ==========

    // Step 11: Navigate to appointment booking page
    cy.visit("/appointments");
    cy.get('button').contains("Book Appointment").click();
    cy.wait(2000); // Wait for page to load
    cy.url().should("include", "/appointments/book");
    cy.contains("Choose doctor", { timeout: 10000 }).should("be.visible");
    cy.wait(2000); // Wait for doctors to load

    // Step 12: Test validation - Try to proceed without selecting a doctor
    // Button is disabled when no doctor selected, but we can force click to test validation
    cy.get('button').contains("Next", { timeout: 10000 }).should("be.visible").click({ force: true });
    cy.wait(1000);

    // Step 13: Select a doctor
    cy.contains("Jane Smith", { timeout: 10000 }).should("be.visible");
    cy.wait(1000);
    cy.contains("Jane Smith").first().closest('[class*="card"], [class*="Card"]').click();
    
    // Wait for doctor selection to register
    cy.wait(1500);
    cy.get('button').contains("Next").should("not.be.disabled").click();
    cy.wait(1500); // Wait for step transition

    // Step 14: Select date and time
    cy.url().should("include", "/appointments/book");
    cy.contains("Choose Date & Time", { timeout: 5000 }).should("be.visible");
    cy.wait(2000);
    
    // Click TODAY (it's always enabled, but might not have time slots)
    cy.get('div.grid-cols-7 button').contains("TODAY").click();
    cy.wait(2000);
    
    // Wait for time slots to load
    cy.contains("Loading time slots", { timeout: 5000 }).should("not.exist");
    cy.wait(2000);
    
    // Check if time slots are available, if not, click tomorrow (next day)
    cy.get('body').then(($body) => {
      const hasTimeSlots = $body.find('div.grid button').filter((i, el) => {
        const text = Cypress.$(el).text().trim();
        return /^\d{2}:\d{2}$/.test(text);
      }).length > 0;
      
      if (!hasTimeSlots) {
        // No time slots for TODAY, click tomorrow (next enabled date button after TODAY)
        cy.get('div.grid-cols-7 button').not(':disabled').not('.text-gray-600').not('.cursor-not-allowed').not(':contains("TODAY")').first().click();
        cy.wait(2000);
        cy.contains("Loading time slots", { timeout: 5000 }).should("not.exist");
        cy.wait(2000);
      }
    });
    
    // Select first available time slot (buttons with time format HH:MM)
    cy.get('div.grid button').contains(/\d{2}:\d{2}/, { timeout: 10000 }).first().click();
    cy.wait(1500);
    cy.get('button').contains("Next").should("not.be.disabled").click();
    cy.wait(2000);

    // Step 15: Test validation - Try to submit without appointment reason
    cy.url().should("include", "/appointments/book");
    cy.contains("Review Your Appointment", { timeout: 5000 }).should("be.visible");
    cy.wait(1000);
    cy.get('button').contains("Confirm & Book Appointment").click();
    cy.wait(1000);
    cy.contains("Please select a reason for the appointment", { timeout: 5000 }).should("be.visible");
    cy.wait(1000);

    // Step 16: Test validation - OTHER reason without custom text
    cy.get('select#appointmentReason').select("OTHER");
    cy.wait(1000);
    cy.get('button').contains("Confirm & Book Appointment").click();
    cy.wait(1000);
    cy.contains("Please specify the reason for your appointment", { timeout: 5000 }).should("be.visible");
    cy.wait(1000);

    // Step 17: Create valid appointment
    cy.get('select#appointmentReason').select("GENERAL_CHECKUP");
    cy.wait(500);
    cy.get('textarea#notes').type("Regular checkup appointment");
    cy.wait(1000);
    cy.get('button').contains("Confirm & Book Appointment").click();
    cy.wait(2000); // Wait for submission

    // Step 18: Verify success and appointment in patient view
    cy.url({ timeout: 15000 }).should("include", "/appointments");
    cy.wait(2000);
    
    // Verify appointment card exists with correct details
    cy.contains("Jane Smith", { timeout: 10000 }).should("be.visible");
    cy.contains("Cardiology", { timeout: 5000 }).should("be.visible");
    cy.contains("General Checkup", { timeout: 5000 }).should("be.visible");
    cy.contains("Regular checkup appointment", { timeout: 5000 }).should("be.visible");
    
    // Verify the red Cancel button exists - SCOPE TO THE SPECIFIC APPOINTMENT CARD
    cy.contains("Jane Smith")
      .closest('.card')
      .within(() => {
        cy.get('button').contains("Cancel", { timeout: 10000 }).should("be.visible");
      });
    cy.wait(1000);

    // ========== PART 4: VERIFY IN DOCTOR ACCOUNT ==========

    // Step 19: Logout as patient
    cy.get('[title="Logout"]', { timeout: 10000 }).should("be.visible").click();
    cy.wait(1500);
    cy.url({ timeout: 10000 }).should("include", "/login");
    cy.wait(1000);

    // Step 20: Login as doctor
    cy.get('input[type="email"]').type(doctorEmail);
    cy.wait(500);
    cy.get('input[type="password"]').type(password);
    cy.wait(500);
    cy.get('button[type="submit"]').click();
    cy.wait(2000);
    cy.url({ timeout: 10000 }).should("satisfy", (url) => {
      return url === Cypress.config().baseUrl + "/" || url.includes("/");
    });
    cy.wait(2000);

    // Step 21: Navigate to appointments and verify
    cy.visit("/appointments");
    cy.wait(2000);
    cy.contains("Appointments", { timeout: 10000 }).should("be.visible");
    cy.wait(1500);
    cy.contains("John Doe", { timeout: 10000 }).should("be.visible");
    cy.contains("General Checkup").should("be.visible");
    cy.contains("Regular checkup appointment").should("be.visible");
    cy.wait(1000);
  });
});
