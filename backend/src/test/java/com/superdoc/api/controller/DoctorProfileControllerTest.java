package com.superdoc.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.superdoc.api.model.dto.ProfileDtos.*;
import com.superdoc.api.service.DoctorProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import static com.superdoc.api.controller.TestAuthHelper.withEmail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DoctorProfileController.class)
@Import(TestSecurityConfig.class)
class DoctorProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DoctorProfileService doctorProfileService;

    @Test
    void shouldReturnOkWhenCreateProfileSucceeds() throws Exception {
        // Arrange
        String email = "doctor@example.com";
        CreateDoctorProfileRequest request = new CreateDoctorProfileRequest(
                "John",
                "Doe",
                "Cardiology",
                "Experienced cardiologist",
                "LIC-12345",
                "City Clinic",
                12,
                null,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                false
        );
        
        DoctorProfileResponse response = new DoctorProfileResponse(
                1L, 1L, "doctor@example.com",
                "John", "Doe", "Cardiology",
                "Experienced cardiologist", "LIC-12345",
                "City Clinic", 12, "Unknown Location",
                null, LocalTime.of(9, 0), LocalTime.of(17, 0),
                false, Instant.now(), Instant.now()
        );
        
        when(doctorProfileService.createProfile(eq(email), any(CreateDoctorProfileRequest.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/doctors/profile")
                        .with(withEmail(email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));

        verify(doctorProfileService).createProfile(eq(email), any(CreateDoctorProfileRequest.class));
    }

    @Test
    void shouldReturnOkWhenUpdateProfileSucceeds() throws Exception {
        // Arrange
        String email = "doctor@example.com";
        UpdateDoctorProfileRequest request = new UpdateDoctorProfileRequest(
                "Jane",
                "Smith",
                "Dermatology",
                "Skin specialist",
                "LIC-98765",
                "Downtown Derm",
                8,
                null,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                false
        );
        
        DoctorProfileResponse response = new DoctorProfileResponse(
                1L, 1L, "doctor@example.com",
                "Jane", "Smith", "Dermatology",
                "Skin specialist", "LIC-98765",
                "Downtown Derm", 8, "Unknown Location",
                null, LocalTime.of(9, 0), LocalTime.of(17, 0),
                false, Instant.now(), Instant.now()
        );
        
        when(doctorProfileService.updateProfile(eq(email), any(UpdateDoctorProfileRequest.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/doctors/profile")
                        .with(withEmail(email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Smith"));

        verify(doctorProfileService).updateProfile(eq(email), any(UpdateDoctorProfileRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenCreateProfileWithMissingFirstName() throws Exception {
        // Arrange
        String email = "doctor@example.com";
        String invalidJson = "{\"lastName\":\"Doe\"}";

        // Act & Assert
        mockMvc.perform(post("/doctors/profile")
                        .with(withEmail(email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verify(doctorProfileService, never()).createProfile(anyString(), any(CreateDoctorProfileRequest.class));
    }

    @Test
    void shouldReturnOkWhenGetMyProfileSucceeds() throws Exception {
        // Arrange
        String email = "doctor@example.com";
        
        DoctorProfileResponse response = new DoctorProfileResponse(
                1L, 1L, "doctor@example.com",
                "John", "Doe", "Cardiology",
                null, null, null, null, null,
                null, null, null,
                false, Instant.now(), Instant.now()
        );
        
        when(doctorProfileService.getProfileByEmail(email)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/doctors/profile/me")
                        .with(withEmail(email))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));

        verify(doctorProfileService).getProfileByEmail(email);
    }

    @Test
    void shouldReturnOkWhenGetAllDoctorsSucceeds() throws Exception {
        // Arrange
        DoctorListResponse doctor1 = new DoctorListResponse(
                1L, "John", "Doe", "Cardiology", "City Clinic", null
        );
        DoctorListResponse doctor2 = new DoctorListResponse(
                2L, "Jane", "Smith", "Dermatology", "Downtown Derm", null
        );
        
        when(doctorProfileService.getAllDoctors()).thenReturn(List.of(doctor1, doctor2));

        // Act & Assert
        mockMvc.perform(get("/doctors")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].firstName").value("John"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].firstName").value("Jane"));

        verify(doctorProfileService).getAllDoctors();
    }
}
