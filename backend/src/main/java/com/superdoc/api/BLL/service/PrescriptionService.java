package com.superdoc.api.service;

import com.superdoc.api.BLL.domain.Prescription;
import com.superdoc.api.BLL.domain.User;
import com.superdoc.api.BLL.domain.Patient;
import com.superdoc.api.enumerate.PrescriptionStatus;
import com.superdoc.api.enumerate.Role;
import com.superdoc.api.model.dto.PrescriptionDtos.*;
import com.superdoc.api.BLL.IRepositories.IPrescriptionRepository;
import com.superdoc.api.BLL.IRepositories.IUserRepository;
import com.superdoc.api.BLL.IRepositories.IPatientProfileRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PrescriptionService {

    private final IPrescriptionRepository prescriptionRepository;
    private final IUserRepository userRepository;
    private final IPatientProfileRepository patientProfileRepository;

    public PrescriptionResponse createPrescription(String doctorEmail, CreatePrescriptionRequest request) {
        // Verify user is a doctor
        User doctor = userRepository.findByEmail(doctorEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        if (doctor.getRole() != Role.DOCTOR) {
            throw new IllegalArgumentException("Only doctors can create prescriptions");
        }

        // The request.patientId() is ALWAYS a PatientProfile ID (from the frontend)
        // Always look up by PatientProfile first to avoid confusion with User IDs
        Patient patientProfile = patientProfileRepository.findById(request.patientId())
                .orElseThrow(() -> new IllegalArgumentException("Patient profile not found with ID: " + request.patientId()));
        
        User patient = patientProfile.getUser();
        if (patient == null) {
            throw new IllegalArgumentException("Patient profile found but has no associated user for ID: " + request.patientId());
        }
        
        // Verify the profile ID matches (double-check)
        if (patientProfile.getId() == null || !patientProfile.getId().equals(request.patientId())) {
            throw new IllegalArgumentException("Patient profile ID mismatch. Expected profile ID: " + request.patientId() +
                ", but profile has ID: " + (patientProfile.getId() != null ? patientProfile.getId() : "null"));
        }
        
        // Log for debugging - show which patient we're creating prescription for
        String profileName = patientProfile.getFirstName() + " " + patientProfile.getLastName();
        System.out.println("Creating prescription for PatientProfile ID " + request.patientId() + 
            " - Name: " + profileName + ", User ID: " + patient.getId() + ", Email: " + patient.getEmail());
        
        if (patient.getRole() != Role.PATIENT) {
            throw new IllegalArgumentException("Patient ID must refer to a patient user. Found role: " + patient.getRole());
        }

        // Validate validUntil is in the future
        if (request.validUntil().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Valid until date must be in the future");
        }

        // Create prescription
        Prescription prescription = Prescription.builder()
                .patient(patient)
                .doctor(doctor)
                .medicationName(request.medicationName())
                .dosage(request.dosage())
                .frequency(request.frequency())
                .duration(request.duration())
                .instructions(request.instructions())
                .status(PrescriptionStatus.DRAFT)
                .validUntil(request.validUntil())
                .issuedAt(java.time.Instant.now())
                .createdAt(java.time.Instant.now())
                .updatedAt(java.time.Instant.now())
                .build();

        Prescription saved = prescriptionRepository.save(prescription);
        return toResponse(saved);
    }

    public PrescriptionResponse updatePrescription(Long prescriptionId, String doctorEmail, UpdatePrescriptionRequest request) {
        // Verify user is a doctor
        User doctor = userRepository.findByEmail(doctorEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        if (doctor.getRole() != Role.DOCTOR) {
            throw new IllegalArgumentException("Only doctors can update prescriptions");
        }

        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Prescription not found"));

        // Verify doctor owns this prescription
        if (!prescription.getDoctor().getId().equals(doctor.getId())) {
            throw new IllegalArgumentException("You can only update prescriptions you created");
        }

        // Cannot edit once ACTIVE
        if (prescription.getStatus() == PrescriptionStatus.ACTIVE) {
            throw new IllegalArgumentException("Cannot edit a prescription that is already ACTIVE. Create a new prescription instead.");
        }

        // Cannot edit if DISCONTINUED or EXPIRED
        if (prescription.getStatus() == PrescriptionStatus.DISCONTINUED || 
            prescription.getStatus() == PrescriptionStatus.EXPIRED) {
            throw new IllegalArgumentException("Cannot edit a prescription that is " + prescription.getStatus() + ". Create a new prescription instead.");
        }

        // Update fields
        if (request.medicationName() != null) {
            prescription.setMedicationName(request.medicationName());
        }
        if (request.dosage() != null) {
            prescription.setDosage(request.dosage());
        }
        if (request.frequency() != null) {
            prescription.setFrequency(request.frequency());
        }
        if (request.duration() != null) {
            prescription.setDuration(request.duration());
        }
        if (request.instructions() != null) {
            prescription.setInstructions(request.instructions());
        }
        if (request.validUntil() != null) {
            if (request.validUntil().isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("Valid until date must be in the future");
            }
            prescription.setValidUntil(request.validUntil());
        }

        Prescription updated = prescriptionRepository.save(prescription);
        return toResponse(updated);
    }

    public PrescriptionResponse activatePrescription(Long prescriptionId, String doctorEmail) {
        // Verify user is a doctor
        User doctor = userRepository.findByEmail(doctorEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        if (doctor.getRole() != Role.DOCTOR) {
            throw new IllegalArgumentException("Only doctors can activate prescriptions");
        }

        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Prescription not found"));

        // Verify doctor owns this prescription
        if (!prescription.getDoctor().getId().equals(doctor.getId())) {
            throw new IllegalArgumentException("You can only activate prescriptions you created");
        }

        // Can only activate DRAFT prescriptions
        if (prescription.getStatus() != PrescriptionStatus.DRAFT) {
            throw new IllegalArgumentException("Can only activate prescriptions with DRAFT status. Current status: " + prescription.getStatus());
        }

        // Validate validUntil is still in the future
        if (prescription.getValidUntil().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot activate prescription with past validUntil date");
        }

        prescription.setStatus(PrescriptionStatus.ACTIVE);
        Prescription saved = prescriptionRepository.save(prescription);
        return toResponse(saved);
    }

    public PrescriptionResponse discontinuePrescription(Long prescriptionId, String doctorEmail) {
        // Verify user is a doctor
        User doctor = userRepository.findByEmail(doctorEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        if (doctor.getRole() != Role.DOCTOR) {
            throw new IllegalArgumentException("Only doctors can discontinue prescriptions");
        }

        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Prescription not found"));

        // Verify doctor owns this prescription
        if (!prescription.getDoctor().getId().equals(doctor.getId())) {
            throw new IllegalArgumentException("You can only discontinue prescriptions you created");
        }

        // Can only discontinue ACTIVE prescriptions
        if (prescription.getStatus() != PrescriptionStatus.ACTIVE) {
            throw new IllegalArgumentException("Can only discontinue ACTIVE prescriptions. Current status: " + prescription.getStatus());
        }

        prescription.setStatus(PrescriptionStatus.DISCONTINUED);
        Prescription saved = prescriptionRepository.save(prescription);
        return toResponse(saved);
    }

    public List<PrescriptionResponse> getDoctorPrescriptions(String doctorEmail) {
        User doctor = userRepository.findByEmail(doctorEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        if (doctor.getRole() != Role.DOCTOR) {
            throw new IllegalArgumentException("Only doctors can view their prescriptions");
        }

        List<Prescription> prescriptions = prescriptionRepository.findByDoctor_IdOrderByCreatedAtDesc(doctor.getId());
        return prescriptions.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<PrescriptionResponse> getPatientPrescriptions(String patientEmail) {
        User patient = userRepository.findByEmail(patientEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        if (patient.getRole() != Role.PATIENT) {
            throw new IllegalArgumentException("Only patients can view their prescriptions");
        }

        // Use email-based query which is more reliable - finds prescriptions by patient email
        // This handles cases where prescriptions might have been created with different ID types
        List<Prescription> prescriptions = prescriptionRepository.findByPatientEmail(patientEmail);
        
        return prescriptions.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public PrescriptionResponse getPrescription(Long prescriptionId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Prescription not found"));

        // Verify access: doctor can see their own, patient can see their own
        boolean isDoctor = user.getRole() == Role.DOCTOR && 
                          user.getId().equals(prescription.getDoctor().getId());
        boolean isPatient = user.getRole() == Role.PATIENT && 
                           user.getId().equals(prescription.getPatient().getId());

        if (!isDoctor && !isPatient) {
            throw new IllegalArgumentException("You don't have permission to view this prescription");
        }

        return toResponse(prescription);
    }

    /**
     * Scheduled task to automatically expire prescriptions past their validUntil date
     * Runs daily at midnight
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void expirePrescriptions() {
        LocalDate today = LocalDate.now();
        List<Prescription> expired = prescriptionRepository.findByStatusAndValidUntilBefore(PrescriptionStatus.ACTIVE, today);
        
        for (Prescription prescription : expired) {
            prescription.setStatus(PrescriptionStatus.EXPIRED);
            prescriptionRepository.save(prescription);
        }
    }

    private PrescriptionResponse toResponse(Prescription prescription) {
        String patientName = prescription.getPatient() != null ? prescription.getPatient().getEmail() : "Unknown";
        // Find patient profile to get name
        if (prescription.getPatient() != null) {
            Patient patientProfile = patientProfileRepository.findAll().stream()
                    .filter(pat -> pat.getUser() != null && pat.getUser().getId().equals(prescription.getPatient().getId()))
                    .findFirst()
                    .orElse(null);
            if (patientProfile != null) {
                patientName = patientProfile.getFirstName() + " " + patientProfile.getLastName();
            } else {
                patientName = prescription.getPatient().getEmail();
            }
        }

        String doctorName = prescription.getDoctor() != null ? prescription.getDoctor().getEmail() : "Unknown";
        // Find doctor profile to get name - need to get all doctors and find by user ID
        // TODO: Optimize this with a findByUserId method
        if (prescription.getDoctor() != null) {
            doctorName = prescription.getDoctor().getEmail(); // Fallback to email
            // Could add doctor profile lookup here if needed, but for now use email
        }

        return new PrescriptionResponse(
                prescription.getId(),
                prescription.getPatient().getId(),
                patientName,
                prescription.getDoctor().getId(),
                doctorName,
                prescription.getMedicationName(),
                prescription.getDosage(),
                prescription.getFrequency(),
                prescription.getDuration(),
                prescription.getInstructions(),
                prescription.getStatus(),
                prescription.getIssuedAt(),
                prescription.getValidUntil(),
                prescription.getCreatedAt(),
                prescription.getUpdatedAt()
        );
    }
}

