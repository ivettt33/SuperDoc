package com.superdoc.api.controller;

import com.superdoc.api.model.dto.OnboardingDtos.*;
import com.superdoc.api.service.OnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/onboarding", produces = "application/json")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService svc;

    @PostMapping(value = "/role", consumes = "application/json")
    public ResponseEntity<Void> updateRole(@RequestBody @Valid RoleSelectionRequest req,
                                          @AuthenticationPrincipal String email) {
        svc.updateUserRole(email, req);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/doctor", consumes = "application/json")
    public ResponseEntity<Void> doctor(@RequestBody @Valid DoctorOnboardRequest req,
                                       @AuthenticationPrincipal String email) {
        svc.onboardDoctor(email, req);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/patient", consumes = "application/json")
    public ResponseEntity<Void> patient(@RequestBody @Valid PatientOnboardRequest req,
                                        @AuthenticationPrincipal String email) {
        svc.onboardPatient(email, req);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/summary")
    public ResponseEntity<OnboardingSummaryResponse> summary(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(svc.getOnboardingSummary(email));
    }
}
