package com.superdoc.api.controller;

import com.superdoc.api.model.dto.AppointmentDtos.*;
import com.superdoc.api.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(value = "/appointments", produces = "application/json")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping(consumes = "application/json")
    @PreAuthorize("hasRole('PATIENT')") // Only patients can book appointments
    public ResponseEntity<AppointmentResponse> createAppointment(
            @RequestBody @Valid CreateAppointmentRequest request,
            @AuthenticationPrincipal String email) {
        AppointmentResponse response = appointmentService.createAppointment(email, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<List<AppointmentResponse>> getMyAppointments(
            @AuthenticationPrincipal String email) {
        List<AppointmentResponse> appointments = appointmentService.getMyAppointments(email);
        return ResponseEntity.ok(appointments);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponse> getAppointment(
            @PathVariable Long id,
            @AuthenticationPrincipal String email) {
        AppointmentResponse response = appointmentService.getAppointment(id, email);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AppointmentResponse> updateAppointment(
            @PathVariable Long id,
            @RequestBody @Valid UpdateAppointmentRequest request,
            @AuthenticationPrincipal String email) {
        AppointmentResponse response = appointmentService.updateAppointment(id, email, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelAppointment(
            @PathVariable Long id,
            @AuthenticationPrincipal String email) {
        appointmentService.cancelAppointment(id, email);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/availability/{doctorId}")
    public ResponseEntity<?> getAvailableTimeSlots(
            @PathVariable Long doctorId,
            @RequestParam String date) {
        try {
            LocalDate localDate = LocalDate.parse(date);
            List<AvailableTimeSlot> slots = appointmentService.getAvailableTimeSlots(doctorId, localDate);
            return ResponseEntity.ok(slots);
        } catch (java.time.format.DateTimeParseException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
