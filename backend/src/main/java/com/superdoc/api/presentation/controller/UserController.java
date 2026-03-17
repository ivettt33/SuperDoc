package com.superdoc.api.controller;

import com.superdoc.api.persistence.entities.UserEntity;
import com.superdoc.api.persistence.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/users", produces = "application/json")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<UserInfoResponse>> getAllUsers() {
        List<UserEntity> users = userRepository.findAll();
        // Eagerly load profiles to avoid lazy loading issues
        users.forEach(user -> {
            if (user.getPatientProfile() != null) {
                user.getPatientProfile().getFirstName(); // Force load
            }
            if (user.getDoctorProfile() != null) {
                user.getDoctorProfile().getFirstName(); // Force load
            }
        });
        List<UserInfoResponse> userInfos = users.stream()
                .map(this::toUserInfo)
                .collect(Collectors.toList());
        return ResponseEntity.ok(userInfos);
    }

    private UserInfoResponse toUserInfo(UserEntity user) {
        String firstName = "";
        String lastName = "";
        Long profileId = null;

        if (user.getPatientProfile() != null) {
            firstName = user.getPatientProfile().getFirstName();
            lastName = user.getPatientProfile().getLastName();
            profileId = user.getPatientProfile().getId();
        } else if (user.getDoctorProfile() != null) {
            firstName = user.getDoctorProfile().getFirstName();
            lastName = user.getDoctorProfile().getLastName();
            profileId = user.getDoctorProfile().getId();
        }

        return new UserInfoResponse(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                firstName,
                lastName,
                profileId
        );
    }

    public record UserInfoResponse(
            Long userId,
            String email,
            String role,
            String firstName,
            String lastName,
            Long profileId
    ) {}
}

