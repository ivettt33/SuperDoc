package com.superdoc.api.controller;

import com.superdoc.api.enumerate.Role;
import com.superdoc.api.persistence.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping(produces = "application/json")
@RequiredArgsConstructor
public class MeController {

    private final UserRepository users;

    public record Me(String email, Role role) {}

    @GetMapping("/me")
    public Me me(@AuthenticationPrincipal String email) {
        if (email == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var u = users.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return new Me(u.getEmail(), u.getRole());
    }
}
