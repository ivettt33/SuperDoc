package com.superdoc.api.service;

import com.superdoc.api.BLL.domain.User;
import com.superdoc.api.enumerate.Role;
import com.superdoc.api.model.dto.AuthDtos.*;
import com.superdoc.api.BLL.IRepositories.IUserRepository;
import com.superdoc.api.BLL.IRepositories.IDoctorProfileRepository;
import com.superdoc.api.BLL.IRepositories.IPatientProfileRepository;
import com.superdoc.api.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthService.class);
    private final IUserRepository users;
    private final IDoctorProfileRepository doctorProfileRepository;
    private final IPatientProfileRepository patientProfileRepository;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final MailService mailService;

    public AuthService(IUserRepository users, IDoctorProfileRepository doctorProfileRepository, IPatientProfileRepository patientProfileRepository, PasswordEncoder encoder, JwtService jwt, MailService mailService) {
        this.users = users;
        this.doctorProfileRepository = doctorProfileRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.encoder = encoder;
        this.jwt = jwt;
        this.mailService = mailService;
    }

    public void register(RegisterRequest req) {
        String email = normalizeEmail(req.email());
        String password = normalizePassword(req.password());
        Role role = req.role();
        if (role == null) {
            throw new IllegalArgumentException("Role is required");
        }

        if (users.existsByEmail(email)) throw new IllegalArgumentException("Email already used");
        var u = User.builder()
                .email(email)
                .passwordHash(encoder.encode(password))
                .role(role)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        users.save(u);
    }

    public JwtResponse login(LoginRequest req) {
        String email = normalizeEmail(req.email());
        String password = normalizePassword(req.password());

        var u = users.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("Bad credentials"));
        if (!encoder.matches(password, u.getPasswordHash()))
            throw new IllegalArgumentException("Bad credentials");

        var token = jwt.generate(Map.of("role", u.getRole().name(), "uid", u.getId()), u.getEmail());
        
        // Check if onboarding is required
        boolean onboardingRequired = false;
        if (u.getRole() == Role.DOCTOR) {
            // Check if doctor profile exists by looking up profile with this user's ID
            onboardingRequired = doctorProfileRepository.findAll().stream()
                    .noneMatch(doc -> doc.getUser() != null && doc.getUser().getId().equals(u.getId()));
        } else if (u.getRole() == Role.PATIENT) {
            // Check if patient profile exists by looking up profile with this user's ID
            onboardingRequired = patientProfileRepository.findAll().stream()
                    .noneMatch(pat -> pat.getUser() != null && pat.getUser().getId().equals(u.getId()));
        }
        
        return new JwtResponse(token, u.getEmail(), u.getRole(), onboardingRequired);
    }

    public void requestPasswordReset(ForgotPasswordRequest req) {
        var userOpt = users.findByEmail(normalizeEmail(req.email()));
        if (userOpt.isEmpty()) return; // avoid leaking whether email exists
        var user = userOpt.get();
        user.setPasswordResetToken(UUID.randomUUID().toString());
        user.setPasswordResetExpiresAt(Instant.now().plusSeconds(60 * 60)); // 1 hour
        users.save(user);
        mailService.sendPasswordResetEmail(user.getEmail(), user.getPasswordResetToken());
        log.info("Password reset requested for {} token={} (dev log)", user.getEmail(), user.getPasswordResetToken());
    }

    public void resetPassword(ResetPasswordRequest req) {
        var user = users.findByPasswordResetToken(req.token())
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));
        var expires = user.getPasswordResetExpiresAt();
        if (expires == null || expires.isBefore(Instant.now())) {
            throw new IllegalArgumentException("Expired token");
        }
        user.setPasswordHash(encoder.encode(normalizePassword(req.newPassword())));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiresAt(null);
        users.save(user);
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Email is required");
        }
        return email.trim().toLowerCase();
    }

    private String normalizePassword(String password) {
        if (!StringUtils.hasText(password)) {
            throw new IllegalArgumentException("Password is required");
        }
        return password.trim();
    }
}
