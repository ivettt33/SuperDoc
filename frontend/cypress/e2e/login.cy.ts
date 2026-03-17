describe("Login flow", () => {
  const testEmail = `test-${Date.now()}@example.com`;
  const testPassword = "TestPassword123!";

  before(() => {
    const apiUrl = "http://localhost:8080";
    // Register a test user
    cy.request({
      method: "POST",
      url: `${apiUrl}/auth/register`,
      body: {
        email: testEmail,
        password: testPassword,
        role: "PATIENT",
      },
      failOnStatusCode: false,
    }).then((res) => {
      // Accept 201 or 400 or handle 500 
      if (res.status === 201 || res.status === 400) {
        // User registered or already exists - continue
        return;
      }
      // For other errors, log but continue - might work if user already exists
      cy.log(`Registration returned status ${res.status}, proceeding anyway`);
    });
  });

  it("logs in successfully with valid credentials", () => {
    cy.visit("/login");

    cy.get('input[type="email"]').type(testEmail);
    cy.get('input[type="password"]').type(testPassword);
    cy.get('button[type="submit"]').click();

    // Assert redirect away from /login
    cy.url().should("not.include", "/login");
    
    // New users are redirected to onboarding, existing users to dashboard
    // Check for either logged-in page (onboarding or dashboard)
    cy.url().should("satisfy", (url) => {
      return url.includes("/onboarding/patient") || url === "http://localhost:5173/";
    });
    
    // If on dashboard, verify it's visible; if on onboarding, that's also a valid logged-in state
    cy.get("body").should("be.visible");
  });
});

