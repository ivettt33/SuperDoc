package com.superdoc.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.superdoc.api.enumerate.Role;
import com.superdoc.api.model.dto.ProfileDtos.*;
import com.superdoc.api.service.PatientProfileService;
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

@WebMvcTest(controllers = PatientProfileController.class)
@Import(TestSecurityConfig.class)
class PatientProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PatientProfileService patientProfileService;

    @Test
    void createProfile_validRequest_returnsOk() throws Exception {
        // Arrange
        String patientEmail = "patient@example.com";
        CreatePatientProfileRequest request = new CreatePatientProfileRequest(
                "John",
                "Doe",
                LocalDate.of(1990, 5, 15),
                "Male",
                "Hypertension",
                "INS123456",
                null
        );

        PatientProfileResponse response = new PatientProfileResponse(
                1L, 1L, "patient@example.com",
                "John", "Doe", LocalDate.of(1990, 5, 15),
                "Male", "Hypertension", "INS123456",
                null, Instant.now(), Instant.now()
        );

        when(patientProfileService.createProfile(eq(patientEmail), any(CreatePatientProfileRequest.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/patients/profile")
                        .with(withEmail(patientEmail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));

        verify(patientProfileService).createProfile(eq(patientEmail), any(CreatePatientProfileRequest.class));
    }

    @Test
    void createProfile_missingRequiredFields_returnsBadRequest() throws Exception {
        // Arrange
        String patientEmail = "patient@example.com";
        String invalidJson = "{\"firstName\":\"John\"}";

        // Act & Assert
        mockMvc.perform(post("/patients/profile")
                        .with(withEmail(patientEmail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verify(patientProfileService, never()).createProfile(anyString(), any(CreatePatientProfileRequest.class));
    }

    @Test
    void getMyProfile_validRequest_returnsOk() throws Exception {
        // Arrange
        String patientEmail = "patient@example.com";
        PatientProfileResponse response = new PatientProfileResponse(
                1L, 1L, "patient@example.com",
                "John", "Doe", LocalDate.of(1990, 5, 15),
                "Male", null, null,
                null, Instant.now(), Instant.now()
        );

        when(patientProfileService.getProfileByEmail(patientEmail)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/patients/profile/me")
                        .with(withEmail(patientEmail))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));

        verify(patientProfileService).getProfileByEmail(patientEmail);
    }

    @Test
    void getProfileById_validRequest_returnsOk() throws Exception {
        // Arrange
        Long profileId = 1L;
        String userEmail = "doctor@example.com";
        PatientProfileResponse response = new PatientProfileResponse(
                profileId, 1L, "patient@example.com",
                "John", "Doe", null,
                null, null, null,
                null, Instant.now(), Instant.now()
        );

        when(patientProfileService.getProfileById(profileId)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/patients/profile/" + profileId)
                        .with(withEmail(userEmail))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(profileId))
                .andExpect(jsonPath("$.firstName").value("John"));

        verify(patientProfileService).getProfileById(profileId);
    }

    @Test
    void getPatientEmailByProfileId_validRequest_returnsOk() throws Exception {
        // Arrange
        Long profileId = 1L;
        String doctorEmail = "doctor@example.com";
        PatientProfileResponse response = new PatientProfileResponse(
                profileId, 1L, "patient@example.com",
                "John", "Doe", null,
                null, null, null,
                null, Instant.now(), Instant.now()
        );

        when(patientProfileService.getProfileById(profileId)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/patients/profile/" + profileId + "/email")
                        .with(withEmail(doctorEmail))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileId").value(profileId))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("patient@example.com"));

        verify(patientProfileService).getProfileById(profileId);
    }

    @Test
    void getPatientEmailByProfileId_profileWithoutUser_returnsBadRequest() throws Exception {
        // Arrange
        Long profileId = 1L;
        String doctorEmail = "doctor@example.com";
        PatientProfileResponse response = new PatientProfileResponse(
                profileId, null, null, // No user
                "John", "Doe", null,
                null, null, null,
                null, Instant.now(), Instant.now()
        );

        when(patientProfileService.getProfileById(profileId)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/patients/profile/" + profileId + "/email")
                        .with(withEmail(doctorEmail))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(patientProfileService).getProfileById(profileId);
    }

    @Test
    void getAllPatients_validRequest_returnsOk() throws Exception {
        // Arrange
        String doctorEmail = "doctor@example.com";
        PatientListResponse summary1 = new PatientListResponse(
                1L, "John", "Doe", "patient1@example.com"
        );
        PatientListResponse summary2 = new PatientListResponse(
                2L, "Jane", "Smith", "patient2@example.com"
        );

        when(patientProfileService.getAllPatients()).thenReturn(List.of(summary1, summary2));

        // Act & Assert
        mockMvc.perform(get("/patients/all")
                        .with(withEmail(doctorEmail))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].profileId").value(1L))
                .andExpect(jsonPath("$[0].firstName").value("John"))
                .andExpect(jsonPath("$[0].lastName").value("Doe"))
                .andExpect(jsonPath("$[0].email").value("patient1@example.com"))
                .andExpect(jsonPath("$[1].profileId").value(2L))
                .andExpect(jsonPath("$[1].firstName").value("Jane"))
                .andExpect(jsonPath("$[1].lastName").value("Smith"));

        verify(patientProfileService).getAllPatients();
    }

    @Test
    void getAllPatients_emptyList_returnsEmptyArray() throws Exception {
        // Arrange
        String doctorEmail = "doctor@example.com";

        when(patientProfileService.getAllPatients()).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/patients/all")
                        .with(withEmail(doctorEmail))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(patientProfileService).getAllPatients();
    }

    @Test
    void createProfile_emptyFirstName_returnsBadRequest() throws Exception {
        // Arrange
        String patientEmail = "patient@example.com";
        CreatePatientProfileRequest request = new CreatePatientProfileRequest(
                "",
                "Doe",
                LocalDate.of(1990, 5, 15),
                "Male",
                null,
                null,
                null
        );

        // Act & Assert
        mockMvc.perform(post("/patients/profile")
                        .with(withEmail(patientEmail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(patientProfileService, never()).createProfile(anyString(), any(CreatePatientProfileRequest.class));
    }
}
