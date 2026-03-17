package com.superdoc.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.superdoc.api.enumerate.AppointmentStatus;
import com.superdoc.api.model.dto.AppointmentDtos.*;
import com.superdoc.api.service.AppointmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static com.superdoc.api.controller.TestAuthHelper.withEmail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AppointmentController.class)
@Import(TestSecurityConfig.class)
class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AppointmentService appointmentService;

    @Test
    void createAppointment_validRequest_returnsCreated() throws Exception {
        // Arrange
        String patientEmail = "patient@test.com";
        LocalDateTime futureDateTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                1L,
                futureDateTime,
                "Regular checkup"
        );

        AppointmentResponse response = new AppointmentResponse(
                100L,
                1L,
                "Dr. Jane Smith",
                "Cardiology",
                1L,
                "John Doe",
                futureDateTime,
                AppointmentStatus.SCHEDULED,
                "Regular checkup",
                java.time.Instant.now(),
                java.time.Instant.now()
        );

        when(appointmentService.createAppointment(eq(patientEmail), any(CreateAppointmentRequest.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/appointments")
                        .with(withEmail(patientEmail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.doctorId").value(1L))
                .andExpect(jsonPath("$.patientId").value(1L))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.notes").value("Regular checkup"));

        verify(appointmentService).createAppointment(eq(patientEmail), any(CreateAppointmentRequest.class));
    }

    @Test
    void createAppointment_missingDoctorId_returnsBadRequest() throws Exception {
        // Arrange
        String patientEmail = "patient@test.com";
        String invalidJson = "{\"appointmentDateTime\":\"2024-12-25T10:00:00\"}";

        // Act & Assert
        mockMvc.perform(post("/appointments")
                        .with(withEmail(patientEmail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verify(appointmentService, never()).createAppointment(anyString(), any());
    }

    @Test
    void createAppointment_pastDateTime_returnsBadRequest() throws Exception {
        // Arrange
        String patientEmail = "patient@test.com";
        LocalDateTime pastDateTime = LocalDateTime.now().minusDays(1);
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                1L,
                pastDateTime,
                null
        );

        // Act & Assert
        mockMvc.perform(post("/appointments")
                        .with(withEmail(patientEmail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(appointmentService, never()).createAppointment(anyString(), any());
    }

    @Test
    void getMyAppointments_validRequest_returnsOk() throws Exception {
        // Arrange
        String userEmail = "patient@test.com";
        LocalDateTime futureDateTime = LocalDateTime.now().plusDays(1);
        AppointmentResponse appointment1 = new AppointmentResponse(
                1L,
                1L,
                "Dr. Jane Smith",
                "Cardiology",
                1L,
                "John Doe",
                futureDateTime,
                AppointmentStatus.SCHEDULED,
                null,
                java.time.Instant.now(),
                java.time.Instant.now()
        );

        when(appointmentService.getMyAppointments(userEmail))
                .thenReturn(List.of(appointment1));

        // Act & Assert
        mockMvc.perform(get("/appointments/me")
                        .with(withEmail(userEmail))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].status").value("SCHEDULED"));

        verify(appointmentService).getMyAppointments(userEmail);
    }

    @Test
    void getAppointment_validRequest_returnsOk() throws Exception {
        // Arrange
        Long appointmentId = 100L;
        String userEmail = "patient@test.com";
        LocalDateTime futureDateTime = LocalDateTime.now().plusDays(1);
        AppointmentResponse response = new AppointmentResponse(
                appointmentId,
                1L,
                "Dr. Jane Smith",
                "Cardiology",
                1L,
                "John Doe",
                futureDateTime,
                AppointmentStatus.SCHEDULED,
                null,
                java.time.Instant.now(),
                java.time.Instant.now()
        );

        when(appointmentService.getAppointment(appointmentId, userEmail))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/appointments/" + appointmentId)
                        .with(withEmail(userEmail))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(appointmentId))
                .andExpect(jsonPath("$.doctorId").value(1L));

        verify(appointmentService).getAppointment(appointmentId, userEmail);
    }

    @Test
    void updateAppointment_validRequest_returnsOk() throws Exception {
        // Arrange
        Long appointmentId = 100L;
        String userEmail = "patient@test.com";
        LocalDateTime newDateTime = LocalDateTime.now().plusDays(2);
        UpdateAppointmentRequest request = new UpdateAppointmentRequest(
                newDateTime,
                AppointmentStatus.CONFIRMED,
                "Updated notes"
        );

        AppointmentResponse response = new AppointmentResponse(
                appointmentId,
                1L,
                "Dr. Jane Smith",
                "Cardiology",
                1L,
                "John Doe",
                newDateTime,
                AppointmentStatus.CONFIRMED,
                "Updated notes",
                java.time.Instant.now(),
                java.time.Instant.now()
        );

        when(appointmentService.updateAppointment(eq(appointmentId), eq(userEmail), any(UpdateAppointmentRequest.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/appointments/" + appointmentId)
                        .with(withEmail(userEmail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.notes").value("Updated notes"));

        verify(appointmentService).updateAppointment(eq(appointmentId), eq(userEmail), any(UpdateAppointmentRequest.class));
    }

    @Test
    void cancelAppointment_validRequest_returnsNoContent() throws Exception {
        // Arrange
        Long appointmentId = 100L;
        String userEmail = "patient@test.com";

        doNothing().when(appointmentService).cancelAppointment(appointmentId, userEmail);

        // Act & Assert
        mockMvc.perform(delete("/appointments/" + appointmentId)
                        .with(withEmail(userEmail)))
                .andExpect(status().isNoContent());

        verify(appointmentService).cancelAppointment(appointmentId, userEmail);
    }

    @Test
    void getAvailableTimeSlots_validRequest_returnsOk() throws Exception {
        // Arrange
        Long doctorId = 1L;
        String date = "2024-12-25";

        AvailableTimeSlot slot1 = new AvailableTimeSlot("09:00", true);
        AvailableTimeSlot slot2 = new AvailableTimeSlot("09:30", true);
        AvailableTimeSlot slot3 = new AvailableTimeSlot("10:00", false);

        when(appointmentService.getAvailableTimeSlots(doctorId, java.time.LocalDate.parse(date)))
                .thenReturn(List.of(slot1, slot2, slot3));

        // Act & Assert
        mockMvc.perform(get("/appointments/availability/" + doctorId)
                        .param("date", date)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].time").value("09:00"))
                .andExpect(jsonPath("$[0].available").value(true))
                .andExpect(jsonPath("$[2].time").value("10:00"))
                .andExpect(jsonPath("$[2].available").value(false));

        verify(appointmentService).getAvailableTimeSlots(doctorId, java.time.LocalDate.parse(date));
    }

    @Test
    void getAvailableTimeSlots_invalidDate_returnsBadRequest() throws Exception {
        // Arrange
        Long doctorId = 1L;
        String invalidDate = "invalid-date";

        // Act & Assert
        mockMvc.perform(get("/appointments/availability/" + doctorId)
                        .param("date", invalidDate)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(appointmentService, never()).getAvailableTimeSlots(anyLong(), any());
    }
}

