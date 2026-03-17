package com.superdoc.api.service;

import com.superdoc.api.enumerate.Role;
import com.superdoc.api.model.dto.AuthDtos.*;
import com.superdoc.api.BLL.domain.User;
import com.superdoc.api.BLL.domain.Doctor;
import com.superdoc.api.BLL.domain.Patient;
import com.superdoc.api.BLL.IRepositories.IUserRepository;
import com.superdoc.api.BLL.IRepositories.IDoctorProfileRepository;
import com.superdoc.api.BLL.IRepositories.IPatientProfileRepository;
import com.superdoc.api.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private IUserRepository users;
    @Mock private IDoctorProfileRepository doctorProfileRepository;
    @Mock private IPatientProfileRepository patientProfileRepository;
    @Mock private PasswordEncoder encoder;
    @Mock private JwtService jwt;
    @Mock private MailService mailService;

    @InjectMocks private AuthService service;

    @Test
    void register_newEmail_savesUserWithEncodedPassword_andRole() {
        // Arrange
        var req = new RegisterRequest("doc@x.com", "password123", Role.DOCTOR);
        when(users.existsByEmail("doc@x.com")).thenReturn(false);
        when(encoder.encode("password123")).thenReturn("hash");

        // Act
        service.register(req);

        // Assert
        var captor = ArgumentCaptor.forClass(User.class);
        verify(users).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("doc@x.com");
        assertThat(saved.getPasswordHash()).isEqualTo("hash");
        assertThat(saved.getRole()).isEqualTo(Role.DOCTOR);
    }

    @Test
    void register_withNullRole_throwsIllegalArgument() {
        // Arrange
        var req = new RegisterRequest("user@x.com", "password123", null);

        // Act & Assert
        assertThatThrownBy(() -> service.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Role");
        verify(users, never()).save(any());
    }

    @Test
    void register_existingEmail_throws() {
        // Arrange
        var req = new RegisterRequest("taken@x.com", "password123", Role.PATIENT);
        when(users.existsByEmail("taken@x.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> service.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email");
        verify(users, never()).save(any());
    }

    @Test
    void login_validCredentials_returnsJwtResponseWithOnboardingFalse() {
        // Arrange
        var req = new LoginRequest("pat@x.com", "password123");
        var u = User.builder()
                .id(42L)
                .email("pat@x.com")
                .passwordHash("hash")
                .role(Role.PATIENT)
                .build();

        when(users.findByEmail("pat@x.com")).thenReturn(Optional.of(u));
        when(encoder.matches("password123", "hash")).thenReturn(true);
        when(jwt.generate(anyMap(), eq("pat@x.com"))).thenReturn("token-123");
        // User is onboarded - patient profile exists
        when(patientProfileRepository.findAll()).thenReturn(List.of(
                Patient.builder().user(u).build()
        ));

        // Act
        JwtResponse resp = service.login(req);

        // Assert
        assertThat(resp.token()).isEqualTo("token-123");
        assertThat(resp.email()).isEqualTo("pat@x.com");
        assertThat(resp.role()).isEqualTo(Role.PATIENT);
        assertThat(resp.onboardingRequired()).isFalse();
    }

    @Test
    void login_doctorWithoutProfile_returnsOnboardingRequired() {
        // Arrange
        var req = new LoginRequest("doc@x.com", "password123");
        var u = User.builder()
                .id(1L)
                .email("doc@x.com")
                .passwordHash("hash")
                .role(Role.DOCTOR)
                .build();

        when(users.findByEmail("doc@x.com")).thenReturn(Optional.of(u));
        when(encoder.matches("password123", "hash")).thenReturn(true);
        when(jwt.generate(anyMap(), eq("doc@x.com"))).thenReturn("token-456");
        // No profile yet
        when(doctorProfileRepository.findAll()).thenReturn(List.of());

        // Act
        JwtResponse resp = service.login(req);

        // Assert
        assertThat(resp.onboardingRequired()).isTrue();
    }

    @Test
    void login_patientWithoutProfile_returnsOnboardingRequired() {
        // Arrange
        var req = new LoginRequest("pat@x.com", "password123");
        var u = User.builder()
                .id(2L)
                .email("pat@x.com")
                .passwordHash("hash")
                .role(Role.PATIENT)
                .build();

        when(users.findByEmail("pat@x.com")).thenReturn(Optional.of(u));
        when(encoder.matches("password123", "hash")).thenReturn(true);
        when(jwt.generate(anyMap(), eq("pat@x.com"))).thenReturn("token-789");
        // No profile yet
        when(patientProfileRepository.findAll()).thenReturn(List.of());

        // Act
        JwtResponse resp = service.login(req);

        // Assert
        assertThat(resp.onboardingRequired()).isTrue();
    }

    @Test
    void login_userNotFound_throws() {
        // Arrange
        var req = new LoginRequest("notfound@x.com", "password123");
        when(users.findByEmail("notfound@x.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.login(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bad credentials");
        verifyNoInteractions(jwt);
    }

    @Test
    void login_wrongPassword_throws() {
        // Arrange
        var req = new LoginRequest("pat@x.com", "wrongpassword");
        var u = User.builder()
                .email("pat@x.com")
                .passwordHash("hash")
                .build();

        when(users.findByEmail("pat@x.com")).thenReturn(Optional.of(u));
        when(encoder.matches("wrongpassword", "hash")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> service.login(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bad credentials");
        verifyNoMoreInteractions(jwt);
    }

    @Test
    void requestPasswordReset_existingUser_setsTokenAndSendsEmail() {
        // Arrange
        var req = new ForgotPasswordRequest("user@x.com");
        var user = User.builder()
                .email("user@x.com")
                .build();
        when(users.findByEmail("user@x.com")).thenReturn(Optional.of(user));

        // Act
        service.requestPasswordReset(req);

        // Assert
        var captor = ArgumentCaptor.forClass(User.class);
        verify(users).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.getPasswordResetToken()).isNotNull();
        assertThat(saved.getPasswordResetExpiresAt()).isNotNull();
        assertThat(saved.getPasswordResetExpiresAt()).isAfter(Instant.now());
        verify(mailService).sendPasswordResetEmail(eq("user@x.com"), anyString());
    }

    @Test
    void requestPasswordReset_nonExistentUser_doesNotThrow() {
        // Arrange
        var req = new ForgotPasswordRequest("nonexistent@x.com");
        when(users.findByEmail("nonexistent@x.com")).thenReturn(Optional.empty());

        // Act
        service.requestPasswordReset(req);

        // Assert - no exception thrown, no email sent
        verify(users, never()).save(any());
        verifyNoInteractions(mailService);
    }

    @Test
    void resetPassword_validToken_updatesPasswordAndClearsToken() {
        // Arrange
        String token = UUID.randomUUID().toString();
        var req = new ResetPasswordRequest(token, "newPassword123");
        var user = User.builder()
                .email("user@x.com")
                .passwordResetToken(token)
                .passwordResetExpiresAt(Instant.now().plusSeconds(3600))
                .build();
        
        when(users.findByPasswordResetToken(token)).thenReturn(Optional.of(user));
        when(encoder.encode("newPassword123")).thenReturn("new-hash");

        // Act
        service.resetPassword(req);

        // Assert
        var captor = ArgumentCaptor.forClass(User.class);
        verify(users).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.getPasswordHash()).isEqualTo("new-hash");
        assertThat(saved.getPasswordResetToken()).isNull();
        assertThat(saved.getPasswordResetExpiresAt()).isNull();
    }

    @Test
    void resetPassword_invalidToken_throws() {
        // Arrange
        String token = "invalid-token";
        var req = new ResetPasswordRequest(token, "newPassword123");
        when(users.findByPasswordResetToken(token)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.resetPassword(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid token");
        verify(users, never()).save(any());
    }

    @Test
    void resetPassword_expiredToken_throws() {
        // Arrange
        String token = UUID.randomUUID().toString();
        var req = new ResetPasswordRequest(token, "newPassword123");
        var user = User.builder()
                .email("user@x.com")
                .passwordResetToken(token)
                .passwordResetExpiresAt(Instant.now().minusSeconds(3600)) // Expired 1 hour ago
                .build();
        
        when(users.findByPasswordResetToken(token)).thenReturn(Optional.of(user));

        // Act & Assert
        assertThatThrownBy(() -> service.resetPassword(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expired token");
        verify(users, never()).save(any());
    }

    @Test
    void resetPassword_nullExpiryTime_throws() {
        // Arrange
        String token = UUID.randomUUID().toString();
        var req = new ResetPasswordRequest(token, "newPassword123");
        var user = User.builder()
                .email("user@x.com")
                .passwordResetToken(token)
                .passwordResetExpiresAt(null) // No expiry set
                .build();
        
        when(users.findByPasswordResetToken(token)).thenReturn(Optional.of(user));

        // Act & Assert
        assertThatThrownBy(() -> service.resetPassword(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expired token");
        verify(users, never()).save(any());
    }
}
