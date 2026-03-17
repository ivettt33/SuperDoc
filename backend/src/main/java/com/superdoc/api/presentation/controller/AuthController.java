package com.superdoc.api.controller;

import com.superdoc.api.model.dto.AuthDtos.JwtResponse;
import com.superdoc.api.model.dto.AuthDtos.LoginRequest;
import com.superdoc.api.model.dto.AuthDtos.RegisterRequest;
import com.superdoc.api.model.dto.AuthDtos.ForgotPasswordRequest;
import com.superdoc.api.model.dto.AuthDtos.ResetPasswordRequest;
import com.superdoc.api.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping(value = "/auth", produces = "application/json")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService auth;

    @PostMapping(value = "/register", consumes = "application/json")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterRequest req,
                                         UriComponentsBuilder uri) {
        auth.register(req);
        return ResponseEntity.created(uri.path("/auth/login").build().toUri()).build();
    }

    @PostMapping(value = "/login", consumes = "application/json")
    public ResponseEntity<JwtResponse> login(@RequestBody @Valid LoginRequest req) {
        return ResponseEntity.ok(auth.login(req));
    }

    @PostMapping(value = "/forgot-password", consumes = "application/json")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forgotPassword(@RequestBody @Valid ForgotPasswordRequest req) {
        try { auth.requestPasswordReset(req); }
        catch (Exception ignored) { /* avoid leaking info and 500s */ }
    }

    @PostMapping(value = "/reset-password", consumes = "application/json")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@RequestBody @Valid ResetPasswordRequest req) {
        try { auth.resetPassword(req); }
        catch (Exception ignored) { /* invalid/expired tokens map to 204 for privacy */ }
    }
}
