package com.superdoc.api.model.dto;

import com.superdoc.api.enumerate.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

public class AuthDtos {
    public record RegisterRequest(@Email @NotBlank String email,
                                  @NotBlank @Size(min = 7, message = "Password must be at least 7 characters long") String password,
                                  @NotNull Role role) {}
    public record LoginRequest(@Email @NotBlank String email,
                               @NotBlank String password) {}
    public record JwtResponse(String token, String email, Role role, boolean onboardingRequired) {}

    public record ForgotPasswordRequest(@Email @NotBlank String email) {}
    public record ResetPasswordRequest(@NotBlank String token,
                                       @NotBlank @Size(min = 7, message = "Password must be at least 7 characters long") String newPassword) {}
}
