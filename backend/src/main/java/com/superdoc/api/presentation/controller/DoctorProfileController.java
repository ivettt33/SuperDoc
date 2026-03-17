package com.superdoc.api.controller;

import com.superdoc.api.model.dto.ProfileDtos.CreateDoctorProfileRequest;
import com.superdoc.api.model.dto.ProfileDtos.UpdateDoctorProfileRequest;
import com.superdoc.api.model.dto.ProfileDtos.DoctorProfileResponse;
import com.superdoc.api.model.dto.ProfileDtos.DoctorListResponse;
import com.superdoc.api.service.DoctorProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/doctors", produces = "application/json")
@RequiredArgsConstructor
public class DoctorProfileController {

    private final DoctorProfileService doctorProfileService;

    @PostMapping(value = "/profile", consumes = "application/json")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<DoctorProfileResponse> createProfile(@RequestBody @Valid CreateDoctorProfileRequest req,
                                                               @AuthenticationPrincipal String email) {
        DoctorProfileResponse response = doctorProfileService.createProfile(email, req);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping(value = "/profile", consumes = "application/json")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<DoctorProfileResponse> updateProfile(@RequestBody @Valid UpdateDoctorProfileRequest req,
                                                               @AuthenticationPrincipal String email) {
        DoctorProfileResponse response = doctorProfileService.updateProfile(email, req);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile/me")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<DoctorProfileResponse> getMyProfile(@AuthenticationPrincipal String email) {
        DoctorProfileResponse response = doctorProfileService.getProfileByEmail(email);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    // No @PreAuthorize - any authenticated user can list doctors
    public ResponseEntity<List<DoctorListResponse>> getAllDoctors() {
        List<DoctorListResponse> doctors = doctorProfileService.getAllDoctors();
        return ResponseEntity.ok(doctors);
    }
}
