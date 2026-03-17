package com.superdoc.api.controller;

import com.superdoc.api.model.dto.PrescriptionDtos.*;
import com.superdoc.api.service.PrescriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/prescriptions", produces = "application/json")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @PostMapping(consumes = "application/json")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<PrescriptionResponse> createPrescription(
            @RequestBody @Valid CreatePrescriptionRequest request,
            @AuthenticationPrincipal String email) {
        PrescriptionResponse response = prescriptionService.createPrescription(email, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<PrescriptionResponse> updatePrescription(
            @PathVariable Long id,
            @RequestBody @Valid UpdatePrescriptionRequest request,
            @AuthenticationPrincipal String email) {
        PrescriptionResponse response = prescriptionService.updatePrescription(id, email, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<PrescriptionResponse> activatePrescription(
            @PathVariable Long id,
            @AuthenticationPrincipal String email) {
        PrescriptionResponse response = prescriptionService.activatePrescription(id, email);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/discontinue")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<PrescriptionResponse> discontinuePrescription(
            @PathVariable Long id,
            @AuthenticationPrincipal String email) {
        PrescriptionResponse response = prescriptionService.discontinuePrescription(id, email);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/doctor")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<PrescriptionResponse>> getDoctorPrescriptions(
            @AuthenticationPrincipal String email) {
        List<PrescriptionResponse> prescriptions = prescriptionService.getDoctorPrescriptions(email);
        return ResponseEntity.ok(prescriptions);
    }

    @GetMapping("/patient")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<PrescriptionResponse>> getPatientPrescriptions(
            @AuthenticationPrincipal String email) {
        List<PrescriptionResponse> prescriptions = prescriptionService.getPatientPrescriptions(email);
        return ResponseEntity.ok(prescriptions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PrescriptionResponse> getPrescription(
            @PathVariable Long id,
            @AuthenticationPrincipal String email) {
        PrescriptionResponse response = prescriptionService.getPrescription(id, email);
        return ResponseEntity.ok(response);
    }
}

