package com.superdoc.api.controller;

import com.superdoc.api.model.dto.ProfileDtos.CreatePatientProfileRequest;
import com.superdoc.api.model.dto.ProfileDtos.UpdatePatientProfileRequest;
import com.superdoc.api.model.dto.ProfileDtos.PatientProfileResponse;
import com.superdoc.api.model.dto.ProfileDtos.PatientListResponse;
import com.superdoc.api.service.PatientProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping(value = "/patients", produces = "application/json")
@RequiredArgsConstructor
public class PatientProfileController {

    private final PatientProfileService patientProfileService;

    @PostMapping(value = "/profile", consumes = "application/json")
    public ResponseEntity<PatientProfileResponse> createProfile(@RequestBody @Valid CreatePatientProfileRequest req,
                                                                 @AuthenticationPrincipal String email) {
        PatientProfileResponse response = patientProfileService.createProfile(email, req);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping(value = "/profile", consumes = "application/json")
    public ResponseEntity<PatientProfileResponse> updateProfile(@RequestBody @Valid UpdatePatientProfileRequest req,
                                                                 @AuthenticationPrincipal String email) {
        PatientProfileResponse response = patientProfileService.updateProfile(email, req);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile/me")
    public ResponseEntity<PatientProfileResponse> getMyProfile(@AuthenticationPrincipal String email) {
        PatientProfileResponse response = patientProfileService.getProfileByEmail(email);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile/{id}")
    @PreAuthorize("hasRole('DOCTOR') or (hasRole('PATIENT') and @patientProfileService.isOwnProfile(#id, authentication.name))")
    public ResponseEntity<PatientProfileResponse> getProfileById(
            @PathVariable Long id,
            @AuthenticationPrincipal String email) {
        PatientProfileResponse response = patientProfileService.getProfileById(id);
        return ResponseEntity.ok(response);
    }
    
    // Temporary debug endpoint to get patient email by profile ID
    @GetMapping("/profile/{id}/email")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<PatientEmailResponse> getPatientEmailByProfileId(@PathVariable Long id) {
        PatientProfileResponse profile = patientProfileService.getProfileById(id);
        if (profile.email() == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(new PatientEmailResponse(
            profile.id(),
            profile.firstName() + " " + profile.lastName(),
            profile.email()
        ));
    }
    
    @GetMapping("/all")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<PatientListResponse>> getAllPatients() {
        List<PatientListResponse> patients = patientProfileService.getAllPatients();
        return ResponseEntity.ok(patients);
    }
    
    public record PatientEmailResponse(Long profileId, String name, String email) {}
}

