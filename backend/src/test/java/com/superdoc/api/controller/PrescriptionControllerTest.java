package com.superdoc.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.superdoc.api.enumerate.PrescriptionStatus;
import com.superdoc.api.model.dto.PrescriptionDtos.*;
import com.superdoc.api.service.PrescriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static com.superdoc.api.controller.TestAuthHelper.withEmail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PrescriptionController.class)
@Import(TestSecurityConfig.class)
class PrescriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PrescriptionService prescriptionService;

    @Test
    void createPrescription_validRequest_returnsCreated() throws Exception {
        // Arrange
        String doctorEmail = "doctor@example.com";
        LocalDate futureDate = LocalDate.now().plusDays(30);
        CreatePrescriptionRequest request = new CreatePrescriptionRequest(
                1L,
                "Aspirin",
                "100mg",
                "Once daily",
                "30 days",
                "Take with food",
                futureDate
        );

        PrescriptionResponse response = new PrescriptionResponse(
                1L,
                1L,
                "John Doe",
                2L,
                "Dr. Jane Smith",
                "Aspirin",
                "100mg",
                "Once daily",
                "30 days",
                "Take with food",
                PrescriptionStatus.DRAFT,
                Instant.now(),
                futureDate,
                Instant.now(),
                Instant.now()
        );

        when(prescriptionService.createPrescription(eq(doctorEmail), any(CreatePrescriptionRequest.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/prescriptions")
                        .with(withEmail(doctorEmail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.medicationName").value("Aspirin"))
                .andExpect(jsonPath("$.dosage").value("100mg"))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        verify(prescriptionService).createPrescription(eq(doctorEmail), any(CreatePrescriptionRequest.class));
    }

    @Test
    void createPrescription_missingRequiredFields_returnsBadRequest() throws Exception {
        // Arrange
        String doctorEmail = "doctor@example.com";
        String invalidJson = "{\"medicationName\":\"Aspirin\"}";

        // Act & Assert
        mockMvc.perform(post("/prescriptions")
                        .with(withEmail(doctorEmail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verify(prescriptionService, never()).createPrescription(anyString(), any(CreatePrescriptionRequest.class));
    }

    @Test
    void createPrescription_pastValidUntilDate_returnsBadRequest() throws Exception {
        // Arrange
        String doctorEmail = "doctor@example.com";
        LocalDate pastDate = LocalDate.now().minusDays(1);
        CreatePrescriptionRequest request = new CreatePrescriptionRequest(
                1L,
                "Aspirin",
                "100mg",
                "Once daily",
                "30 days",
                "Take with food",
                pastDate
        );

        // Act & Assert
        // The @Future validation on validUntil will reject past dates before reaching the service
        mockMvc.perform(post("/prescriptions")
                        .with(withEmail(doctorEmail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // Validation failure

        // Service should not be called due to validation failure
        verify(prescriptionService, never()).createPrescription(anyString(), any(CreatePrescriptionRequest.class));
    }

    @Test
    void updatePrescription_validRequest_returnsOk() throws Exception {
        // Arrange
        Long prescriptionId = 1L;
        String doctorEmail = "doctor@example.com";
        LocalDate futureDate = LocalDate.now().plusDays(30);
        UpdatePrescriptionRequest request = new UpdatePrescriptionRequest(
                "Ibuprofen",
                "200mg",
                "Twice daily",
                "15 days",
                "Updated instructions",
                futureDate
        );

        PrescriptionResponse response = new PrescriptionResponse(
                prescriptionId,
                1L,
                "John Doe",
                2L,
                "Dr. Jane Smith",
                "Ibuprofen",
                "200mg",
                "Twice daily",
                "15 days",
                "Updated instructions",
                PrescriptionStatus.DRAFT,
                Instant.now(),
                futureDate,
                Instant.now(),
                Instant.now()
        );

        when(prescriptionService.updatePrescription(eq(prescriptionId), eq(doctorEmail), any(UpdatePrescriptionRequest.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/prescriptions/" + prescriptionId)
                        .with(withEmail(doctorEmail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.medicationName").value("Ibuprofen"))
                .andExpect(jsonPath("$.dosage").value("200mg"));

        verify(prescriptionService).updatePrescription(eq(prescriptionId), eq(doctorEmail), any(UpdatePrescriptionRequest.class));
    }

    @Test
    void updatePrescription_notOwner_returnsBadRequest() throws Exception {
        // Arrange
        Long prescriptionId = 1L;
        String doctorEmail = "doctor@example.com";
        UpdatePrescriptionRequest request = new UpdatePrescriptionRequest(
                "Ibuprofen",
                null,
                null,
                null,
                null,
                null
        );

        when(prescriptionService.updatePrescription(eq(prescriptionId), eq(doctorEmail), any(UpdatePrescriptionRequest.class)))
                .thenThrow(new IllegalArgumentException("You can only update prescriptions you created"));

        // Act & Assert
        mockMvc.perform(put("/prescriptions/" + prescriptionId)
                        .with(withEmail(doctorEmail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(prescriptionService).updatePrescription(eq(prescriptionId), eq(doctorEmail), any(UpdatePrescriptionRequest.class));
    }

    @Test
    void activatePrescription_validRequest_returnsOk() throws Exception {
        // Arrange
        Long prescriptionId = 1L;
        String doctorEmail = "doctor@example.com";
        LocalDate futureDate = LocalDate.now().plusDays(30);
        PrescriptionResponse response = new PrescriptionResponse(
                prescriptionId,
                1L,
                "John Doe",
                2L,
                "Dr. Jane Smith",
                "Aspirin",
                "100mg",
                "Once daily",
                "30 days",
                "Take with food",
                PrescriptionStatus.ACTIVE,
                Instant.now(),
                futureDate,
                Instant.now(),
                Instant.now()
        );

        when(prescriptionService.activatePrescription(prescriptionId, doctorEmail))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/prescriptions/" + prescriptionId + "/activate")
                        .with(withEmail(doctorEmail))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(prescriptionService).activatePrescription(prescriptionId, doctorEmail);
    }

    @Test
    void activatePrescription_notDraftStatus_returnsBadRequest() throws Exception {
        // Arrange
        Long prescriptionId = 1L;
        String doctorEmail = "doctor@example.com";

        when(prescriptionService.activatePrescription(prescriptionId, doctorEmail))
                .thenThrow(new IllegalArgumentException("Can only activate prescriptions with DRAFT status. Current status: ACTIVE"));

        // Act & Assert
        mockMvc.perform(post("/prescriptions/" + prescriptionId + "/activate")
                        .with(withEmail(doctorEmail))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(prescriptionService).activatePrescription(prescriptionId, doctorEmail);
    }

    @Test
    void discontinuePrescription_validRequest_returnsOk() throws Exception {
        // Arrange
        Long prescriptionId = 1L;
        String doctorEmail = "doctor@example.com";
        LocalDate futureDate = LocalDate.now().plusDays(30);
        PrescriptionResponse response = new PrescriptionResponse(
                prescriptionId,
                1L,
                "John Doe",
                2L,
                "Dr. Jane Smith",
                "Aspirin",
                "100mg",
                "Once daily",
                "30 days",
                "Take with food",
                PrescriptionStatus.DISCONTINUED,
                Instant.now(),
                futureDate,
                Instant.now(),
                Instant.now()
        );

        when(prescriptionService.discontinuePrescription(prescriptionId, doctorEmail))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/prescriptions/" + prescriptionId + "/discontinue")
                        .with(withEmail(doctorEmail))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISCONTINUED"));

        verify(prescriptionService).discontinuePrescription(prescriptionId, doctorEmail);
    }

    @Test
    void discontinuePrescription_notActiveStatus_returnsBadRequest() throws Exception {
        // Arrange
        Long prescriptionId = 1L;
        String doctorEmail = "doctor@example.com";

        when(prescriptionService.discontinuePrescription(prescriptionId, doctorEmail))
                .thenThrow(new IllegalArgumentException("Can only discontinue ACTIVE prescriptions. Current status: DRAFT"));

        // Act & Assert
        mockMvc.perform(post("/prescriptions/" + prescriptionId + "/discontinue")
                        .with(withEmail(doctorEmail))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(prescriptionService).discontinuePrescription(prescriptionId, doctorEmail);
    }

    @Test
    void getDoctorPrescriptions_validRequest_returnsOk() throws Exception {
        // Arrange
        String doctorEmail = "doctor@example.com";
        LocalDate futureDate = LocalDate.now().plusDays(30);
        PrescriptionResponse prescription1 = new PrescriptionResponse(
                1L,
                1L,
                "John Doe",
                2L,
                "Dr. Jane Smith",
                "Aspirin",
                "100mg",
                "Once daily",
                "30 days",
                "Take with food",
                PrescriptionStatus.DRAFT,
                Instant.now(),
                futureDate,
                Instant.now(),
                Instant.now()
        );

        PrescriptionResponse prescription2 = new PrescriptionResponse(
                2L,
                1L,
                "John Doe",
                2L,
                "Dr. Jane Smith",
                "Ibuprofen",
                "200mg",
                "Twice daily",
                "15 days",
                null,
                PrescriptionStatus.ACTIVE,
                Instant.now(),
                futureDate,
                Instant.now(),
                Instant.now()
        );

        when(prescriptionService.getDoctorPrescriptions(doctorEmail))
                .thenReturn(List.of(prescription1, prescription2));

        // Act & Assert
        mockMvc.perform(get("/prescriptions/doctor")
                        .with(withEmail(doctorEmail))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].status").value("DRAFT"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].status").value("ACTIVE"));

        verify(prescriptionService).getDoctorPrescriptions(doctorEmail);
    }

    @Test
    void getDoctorPrescriptions_emptyList_returnsEmptyArray() throws Exception {
        // Arrange
        String doctorEmail = "doctor@example.com";

        when(prescriptionService.getDoctorPrescriptions(doctorEmail))
                .thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/prescriptions/doctor")
                        .with(withEmail(doctorEmail))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(prescriptionService).getDoctorPrescriptions(doctorEmail);
    }

    @Test
    void getPatientPrescriptions_validRequest_returnsOk() throws Exception {
        // Arrange
        String patientEmail = "patient@example.com";
        LocalDate futureDate = LocalDate.now().plusDays(30);
        PrescriptionResponse prescription = new PrescriptionResponse(
                1L,
                1L,
                "John Doe",
                2L,
                "Dr. Jane Smith",
                "Aspirin",
                "100mg",
                "Once daily",
                "30 days",
                "Take with food",
                PrescriptionStatus.ACTIVE,
                Instant.now(),
                futureDate,
                Instant.now(),
                Instant.now()
        );

        when(prescriptionService.getPatientPrescriptions(patientEmail))
                .thenReturn(List.of(prescription));

        // Act & Assert
        mockMvc.perform(get("/prescriptions/patient")
                        .with(withEmail(patientEmail))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].patientName").value("John Doe"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));

        verify(prescriptionService).getPatientPrescriptions(patientEmail);
    }

    @Test
    void getPrescription_validRequest_returnsOk() throws Exception {
        // Arrange
        Long prescriptionId = 1L;
        String userEmail = "patient@example.com";
        LocalDate futureDate = LocalDate.now().plusDays(30);
        PrescriptionResponse response = new PrescriptionResponse(
                prescriptionId,
                1L,
                "John Doe",
                2L,
                "Dr. Jane Smith",
                "Aspirin",
                "100mg",
                "Once daily",
                "30 days",
                "Take with food",
                PrescriptionStatus.ACTIVE,
                Instant.now(),
                futureDate,
                Instant.now(),
                Instant.now()
        );

        when(prescriptionService.getPrescription(prescriptionId, userEmail))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/prescriptions/" + prescriptionId)
                        .with(withEmail(userEmail))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(prescriptionId))
                .andExpect(jsonPath("$.medicationName").value("Aspirin"))
                .andExpect(jsonPath("$.doctorName").value("Dr. Jane Smith"));

        verify(prescriptionService).getPrescription(prescriptionId, userEmail);
    }

    @Test
    void getPrescription_notFound_returnsBadRequest() throws Exception {
        // Arrange
        Long prescriptionId = 999L;
        String userEmail = "patient@example.com";

        when(prescriptionService.getPrescription(prescriptionId, userEmail))
                .thenThrow(new IllegalArgumentException("Prescription not found"));

        // Act & Assert
        mockMvc.perform(get("/prescriptions/" + prescriptionId)
                        .with(withEmail(userEmail))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(prescriptionService).getPrescription(prescriptionId, userEmail);
    }

    @Test
    void getPrescription_noPermission_returnsBadRequest() throws Exception {
        // Arrange
        Long prescriptionId = 1L;
        String userEmail = "unauthorized@example.com";

        when(prescriptionService.getPrescription(prescriptionId, userEmail))
                .thenThrow(new IllegalArgumentException("You don't have permission to view this prescription"));

        // Act & Assert
        mockMvc.perform(get("/prescriptions/" + prescriptionId)
                        .with(withEmail(userEmail))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(prescriptionService).getPrescription(prescriptionId, userEmail);
    }

    @Test
    void updatePrescription_pastValidUntilDate_returnsBadRequest() throws Exception {
        // Arrange
        Long prescriptionId = 1L;
        String doctorEmail = "doctor@example.com";
        LocalDate pastDate = LocalDate.now().minusDays(1);
        UpdatePrescriptionRequest request = new UpdatePrescriptionRequest(
                null,
                null,
                null,
                null,
                null,
                pastDate
        );

        // Act & Assert
        // The @Future validation on validUntil will reject past dates before reaching the service
        mockMvc.perform(put("/prescriptions/" + prescriptionId)
                        .with(withEmail(doctorEmail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // Validation failure

        // Service should not be called due to validation failure
        verify(prescriptionService, never()).updatePrescription(anyLong(), anyString(), any(UpdatePrescriptionRequest.class));
    }
}

