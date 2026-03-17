package com.superdoc.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.superdoc.api.enumerate.Role;
import com.superdoc.api.model.dto.OnboardingDtos.*;
import com.superdoc.api.service.OnboardingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static com.superdoc.api.controller.TestAuthHelper.withEmail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = OnboardingController.class)
@Import(TestSecurityConfig.class)
class OnboardingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OnboardingService onboardingService;

    @Test
    void shouldReturnOkWhenUpdateRoleToDoctorSucceeds() throws Exception {
        // Arrange
        String email = "user@example.com";
        RoleSelectionRequest request = new RoleSelectionRequest(Role.DOCTOR);
        doNothing().when(onboardingService).updateUserRole(eq(email), any(RoleSelectionRequest.class));

        // Act & Assert
        mockMvc.perform(post("/onboarding/role")
                        .with(withEmail(email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(onboardingService).updateUserRole(eq(email), any(RoleSelectionRequest.class));
    }

    @Test
    void shouldReturnOkWhenUpdateRoleToPatientSucceeds() throws Exception {
        // Arrange
        String email = "user@example.com";
        RoleSelectionRequest request = new RoleSelectionRequest(Role.PATIENT);
        doNothing().when(onboardingService).updateUserRole(eq(email), any(RoleSelectionRequest.class));

        // Act & Assert
        mockMvc.perform(post("/onboarding/role")
                        .with(withEmail(email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(onboardingService).updateUserRole(eq(email), any(RoleSelectionRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenUpdateRoleWithNullRole() throws Exception {
        // Arrange
        String email = "user@example.com";
        String invalidJson = "{}";

        // Act & Assert
        mockMvc.perform(post("/onboarding/role")
                        .with(withEmail(email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verify(onboardingService, never()).updateUserRole(anyString(), any(RoleSelectionRequest.class));
    }

    @Test
    void shouldReturnOkWhenOnboardDoctorSucceeds() throws Exception {
        // Arrange
        String email = "doctor@example.com";
        DoctorOnboardRequest request = new DoctorOnboardRequest(
                "John",
                "Doe",
                "Cardiology",
                "Experienced cardiologist",
                "LIC-12345",
                "City Clinic",
                10,
                null,
                java.time.LocalTime.of(9, 0),
                java.time.LocalTime.of(17, 0),
                false
        );
        doNothing().when(onboardingService).onboardDoctor(eq(email), any(DoctorOnboardRequest.class));

        // Act & Assert
        mockMvc.perform(post("/onboarding/doctor")
                        .with(withEmail(email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(onboardingService).onboardDoctor(eq(email), any(DoctorOnboardRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenOnboardDoctorWithMissingFirstName() throws Exception {
        // Arrange
        String email = "doctor@example.com";
        String invalidJson = "{\"lastName\":\"Doe\"}";

        // Act & Assert
        mockMvc.perform(post("/onboarding/doctor")
                        .with(withEmail(email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verify(onboardingService, never()).onboardDoctor(anyString(), any(DoctorOnboardRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenOnboardDoctorWithEmptyLastName() throws Exception {
        // Arrange
        String email = "doctor@example.com";
        DoctorOnboardRequest request = new DoctorOnboardRequest(
                "John",
                "",
                "Cardiology",
                null,
                "LIC-12345",
                "City Clinic",
                10,
                null,
                java.time.LocalTime.of(9, 0),
                java.time.LocalTime.of(17, 0),
                false
        );

        // Act & Assert
        mockMvc.perform(post("/onboarding/doctor")
                        .with(withEmail(email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(onboardingService, never()).onboardDoctor(anyString(), any(DoctorOnboardRequest.class));
    }

    @Test
    void shouldReturnOkWhenOnboardPatientSucceeds() throws Exception {
        // Arrange
        String email = "patient@example.com";
        PatientOnboardRequest request = new PatientOnboardRequest(
                "Jane",
                "Smith",
                LocalDate.of(1990, 1, 1),
                "Female",
                "None",
                "INS-123",
                "photo.jpg"
        );
        doNothing().when(onboardingService).onboardPatient(eq(email), any(PatientOnboardRequest.class));

        // Act & Assert
        mockMvc.perform(post("/onboarding/patient")
                        .with(withEmail(email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(onboardingService).onboardPatient(eq(email), any(PatientOnboardRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenOnboardPatientWithMissingRequiredFields() throws Exception {
        // Arrange
        String email = "patient@example.com";
        String invalidJson = "{\"firstName\":\"Jane\"}";

        // Act & Assert
        mockMvc.perform(post("/onboarding/patient")
                        .with(withEmail(email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verify(onboardingService, never()).onboardPatient(anyString(), any(PatientOnboardRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenOnboardPatientWithNullDateOfBirth() throws Exception {
        // Arrange
        String email = "patient@example.com";
        String invalidJson = "{\"firstName\":\"Jane\",\"lastName\":\"Smith\",\"gender\":\"Female\"}";

        // Act & Assert
        mockMvc.perform(post("/onboarding/patient")
                        .with(withEmail(email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verify(onboardingService, never()).onboardPatient(anyString(), any(PatientOnboardRequest.class));
    }

    @Test
    void shouldReturnOkWhenGetSummarySucceeds() throws Exception {
        // Arrange
        String email = "user@example.com";
        OnboardingSummaryResponse response = new OnboardingSummaryResponse(
                "John",
                "Doe",
                Role.DOCTOR,
                null,
                null
        );
        when(onboardingService.getOnboardingSummary(email)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/onboarding/summary")
                        .with(withEmail(email))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.role").value("DOCTOR"));

        verify(onboardingService).getOnboardingSummary(email);
    }

    @Test
    void shouldReturnOkWithEmptyDataWhenUserNotOnboarded() throws Exception {
        // Arrange
        String email = "newuser@example.com";
        OnboardingSummaryResponse response = new OnboardingSummaryResponse(
                "",
                "",
                Role.PATIENT,
                null,
                null
        );
        when(onboardingService.getOnboardingSummary(email)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/onboarding/summary")
                        .with(withEmail(email))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value(""))
                .andExpect(jsonPath("$.lastName").value(""))
                .andExpect(jsonPath("$.role").value("PATIENT"));

        verify(onboardingService).getOnboardingSummary(email);
    }

    
}

