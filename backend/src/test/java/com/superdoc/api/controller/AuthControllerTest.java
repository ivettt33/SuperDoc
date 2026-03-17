package com.superdoc.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.superdoc.api.enumerate.Role;
import com.superdoc.api.model.dto.AuthDtos.*;
import com.superdoc.api.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@Import(TestSecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    void shouldReturnCreatedWhenUserRegistersSuccessfully() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest("user@example.com", "password123", Role.PATIENT);
        doNothing().when(authService).register(any(RegisterRequest.class));

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenRegisterWithInvalidEmail() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest("invalid-email", "password123", Role.PATIENT);

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenRegisterWithShortPassword() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest("user@example.com", "short", Role.PATIENT);

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenRegisterWithMissingFields() throws Exception {
        // Arrange
        String invalidJson = "{\"email\":\"user@example.com\"}";

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    void shouldReturnOkWhenUserLoginsSuccessfully() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("user@example.com", "password123");
        JwtResponse response = new JwtResponse("jwt-token", "user@example.com", Role.PATIENT, false);
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.role").value("PATIENT"))
                .andExpect(jsonPath("$.onboardingRequired").value(false));

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    void shouldReturnOkWithOnboardingRequiredWhenDoctorNotOnboarded() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("doctor@example.com", "password123");
        JwtResponse response = new JwtResponse("jwt-token", "doctor@example.com", Role.DOCTOR, true);
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.email").value("doctor@example.com"))
                .andExpect(jsonPath("$.role").value("DOCTOR"))
                .andExpect(jsonPath("$.onboardingRequired").value(true));

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenLoginWithInvalidEmail() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("invalid-email", "password123");

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).login(any(LoginRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenLoginWithMissingPassword() throws Exception {
        // Arrange
        String invalidJson = "{\"email\":\"user@example.com\"}";

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verify(authService, never()).login(any(LoginRequest.class));
    }

    @Test
    void shouldReturnNoContentWhenForgotPasswordRequestSucceeds() throws Exception {
        // Arrange
        ForgotPasswordRequest request = new ForgotPasswordRequest("user@example.com");
        doNothing().when(authService).requestPasswordReset(any(ForgotPasswordRequest.class));

        // Act & Assert
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(authService).requestPasswordReset(any(ForgotPasswordRequest.class));
    }

    @Test
    void shouldReturnNoContentWhenForgotPasswordRequestFailsSilently() throws Exception {
        // Arrange
        ForgotPasswordRequest request = new ForgotPasswordRequest("nonexistent@example.com");
        doThrow(new IllegalArgumentException("User not found")).when(authService).requestPasswordReset(any(ForgotPasswordRequest.class));

        // Act & Assert - controller catches exception and returns 204 to avoid info leaking
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(authService).requestPasswordReset(any(ForgotPasswordRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenForgotPasswordWithInvalidEmail() throws Exception {
        // Arrange
        ForgotPasswordRequest request = new ForgotPasswordRequest("invalid-email");

        // Act & Assert
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).requestPasswordReset(any(ForgotPasswordRequest.class));
    }

    @Test
    void shouldReturnNoContentWhenResetPasswordSucceeds() throws Exception {
        // Arrange
        ResetPasswordRequest request = new ResetPasswordRequest("valid-token", "newPassword123");
        doNothing().when(authService).resetPassword(any(ResetPasswordRequest.class));

        // Act & Assert
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(authService).resetPassword(any(ResetPasswordRequest.class));
    }

    @Test
    void shouldReturnNoContentWhenResetPasswordWithInvalidTokenSilently() throws Exception {
        // Arrange
        ResetPasswordRequest request = new ResetPasswordRequest("invalid-token", "newPassword123");
        doThrow(new IllegalArgumentException("Invalid token")).when(authService).resetPassword(any(ResetPasswordRequest.class));

        // Act & Assert - controller catches exception and returns 204 for privacy
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(authService).resetPassword(any(ResetPasswordRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenResetPasswordWithShortPassword() throws Exception {
        // Arrange
        ResetPasswordRequest request = new ResetPasswordRequest("valid-token", "short");

        // Act & Assert
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).resetPassword(any(ResetPasswordRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenResetPasswordWithMissingToken() throws Exception {
        // Arrange
        String invalidJson = "{\"newPassword\":\"newPassword123\"}";

        // Act & Assert
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verify(authService, never()).resetPassword(any(ResetPasswordRequest.class));
    }
}
