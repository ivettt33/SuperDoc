package com.superdoc.api.controller;

import com.superdoc.api.enumerate.Role;
import com.superdoc.api.persistence.entities.DoctorProfileEntity;
import com.superdoc.api.persistence.entities.PatientProfileEntity;
import com.superdoc.api.persistence.entities.UserEntity;
import com.superdoc.api.persistence.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.superdoc.api.controller.TestAuthHelper.withEmail;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@Import(TestSecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @Test
    void getAllUsers_doctorRole_returnsOk() throws Exception {
        // Arrange
        String doctorEmail = "doctor@example.com";

        // Create test users
        UserEntity patient1 = createUser(1L, "patient1@example.com", Role.PATIENT);
        PatientProfileEntity patientProfile1 = new PatientProfileEntity();
        patientProfile1.setId(1L);
        patientProfile1.setFirstName("John");
        patientProfile1.setLastName("Doe");
        patientProfile1.setUser(patient1);
        patient1.setPatientProfile(patientProfile1);

        UserEntity patient2 = createUser(2L, "patient2@example.com", Role.PATIENT);
        PatientProfileEntity patientProfile2 = new PatientProfileEntity();
        patientProfile2.setId(2L);
        patientProfile2.setFirstName("Jane");
        patientProfile2.setLastName("Smith");
        patientProfile2.setUser(patient2);
        patient2.setPatientProfile(patientProfile2);

        UserEntity doctor = createUser(3L, "doctor2@example.com", Role.DOCTOR);
        DoctorProfileEntity doctorProfile = new DoctorProfileEntity();
        doctorProfile.setId(3L);
        doctorProfile.setFirstName("Dr. Bob");
        doctorProfile.setLastName("Johnson");
        doctorProfile.setUser(doctor);
        doctor.setDoctorProfile(doctorProfile);

        when(userRepository.findAll()).thenReturn(List.of(patient1, patient2, doctor));

        // Act & Assert
        mockMvc.perform(get("/users")
                        .with(withEmail(doctorEmail))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].userId").value(1L))
                .andExpect(jsonPath("$[0].email").value("patient1@example.com"))
                .andExpect(jsonPath("$[0].role").value("PATIENT"))
                .andExpect(jsonPath("$[0].firstName").value("John"))
                .andExpect(jsonPath("$[0].lastName").value("Doe"))
                .andExpect(jsonPath("$[0].profileId").value(1L))
                .andExpect(jsonPath("$[1].firstName").value("Jane"))
                .andExpect(jsonPath("$[2].role").value("DOCTOR"))
                .andExpect(jsonPath("$[2].firstName").value("Dr. Bob"));

        verify(userRepository).findAll();
    }

    @Test
    void getAllUsers_usersWithNoProfiles_returnsUsersWithEmptyFields() throws Exception {
        // Arrange
        String doctorEmail = "doctor@example.com";

        UserEntity user1 = createUser(1L, "user1@example.com", Role.PATIENT);
        // No profile set
        user1.setPatientProfile(null);
        user1.setDoctorProfile(null);

        UserEntity user2 = createUser(2L, "user2@example.com", Role.DOCTOR);
        // No profile set
        user2.setPatientProfile(null);
        user2.setDoctorProfile(null);

        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        // Act & Assert
        mockMvc.perform(get("/users")
                        .with(withEmail(doctorEmail))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userId").value(1L))
                .andExpect(jsonPath("$[0].email").value("user1@example.com"))
                .andExpect(jsonPath("$[0].role").value("PATIENT"))
                .andExpect(jsonPath("$[0].firstName").value(""))
                .andExpect(jsonPath("$[0].lastName").value(""))
                .andExpect(jsonPath("$[0].profileId").isEmpty())
                .andExpect(jsonPath("$[1].role").value("DOCTOR"))
                .andExpect(jsonPath("$[1].firstName").value(""));

        verify(userRepository).findAll();
    }

    @Test
    void getAllUsers_usersWithDoctorProfiles_returnsCorrectInfo() throws Exception {
        // Arrange
        String doctorEmail = "doctor@example.com";

        UserEntity doctor = createUser(1L, "doctor@example.com", Role.DOCTOR);
        DoctorProfileEntity doctorProfile = new DoctorProfileEntity();
        doctorProfile.setId(10L);
        doctorProfile.setFirstName("Dr. Alice");
        doctorProfile.setLastName("Williams");
        doctorProfile.setUser(doctor);
        doctor.setDoctorProfile(doctorProfile);

        when(userRepository.findAll()).thenReturn(List.of(doctor));

        // Act & Assert
        mockMvc.perform(get("/users")
                        .with(withEmail(doctorEmail))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstName").value("Dr. Alice"))
                .andExpect(jsonPath("$[0].lastName").value("Williams"))
                .andExpect(jsonPath("$[0].profileId").value(10L));

        verify(userRepository).findAll();
    }

    @Test
    void getAllUsers_usersWithPatientProfiles_returnsCorrectInfo() throws Exception {
        // Arrange
        String doctorEmail = "doctor@example.com";

        UserEntity patient = createUser(1L, "patient@example.com", Role.PATIENT);
        PatientProfileEntity patientProfile = new PatientProfileEntity();
        patientProfile.setId(20L);
        patientProfile.setFirstName("Patient");
        patientProfile.setLastName("User");
        patientProfile.setUser(patient);
        patient.setPatientProfile(patientProfile);

        when(userRepository.findAll()).thenReturn(List.of(patient));

        // Act & Assert
        mockMvc.perform(get("/users")
                        .with(withEmail(doctorEmail))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstName").value("Patient"))
                .andExpect(jsonPath("$[0].lastName").value("User"))
                .andExpect(jsonPath("$[0].profileId").value(20L));

        verify(userRepository).findAll();
    }

    @Test
    void getAllUsers_emptyList_returnsEmptyArray() throws Exception {
        // Arrange
        String doctorEmail = "doctor@example.com";

        when(userRepository.findAll()).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/users")
                        .with(withEmail(doctorEmail))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(userRepository).findAll();
    }

    @Test
    void getAllUsers_mixedProfiles_handlesCorrectly() throws Exception {
        // Arrange
        String doctorEmail = "doctor@example.com";

        // User with patient profile
        UserEntity patientUser = createUser(1L, "patient@example.com", Role.PATIENT);
        PatientProfileEntity patientProfile = new PatientProfileEntity();
        patientProfile.setId(1L);
        patientProfile.setFirstName("John");
        patientProfile.setLastName("Doe");
        patientProfile.setUser(patientUser);
        patientUser.setPatientProfile(patientProfile);

        // User with doctor profile
        UserEntity doctorUser = createUser(2L, "doctor@example.com", Role.DOCTOR);
        DoctorProfileEntity doctorProfile = new DoctorProfileEntity();
        doctorProfile.setId(2L);
        doctorProfile.setFirstName("Dr. Jane");
        doctorProfile.setLastName("Smith");
        doctorProfile.setUser(doctorUser);
        doctorUser.setDoctorProfile(doctorProfile);

        // User with no profile
        UserEntity userNoProfile = createUser(3L, "noprofile@example.com", Role.PATIENT);
        userNoProfile.setPatientProfile(null);
        userNoProfile.setDoctorProfile(null);

        when(userRepository.findAll()).thenReturn(List.of(patientUser, doctorUser, userNoProfile));

        // Act & Assert
        mockMvc.perform(get("/users")
                        .with(withEmail(doctorEmail))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].firstName").value("John")) // Patient profile
                .andExpect(jsonPath("$[1].firstName").value("Dr. Jane")) // Doctor profile
                .andExpect(jsonPath("$[2].firstName").value("")); // No profile

        verify(userRepository).findAll();
    }

    private UserEntity createUser(Long id, String email, Role role) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail(email);
        user.setRole(role);
        user.setPasswordHash("hashedPassword");
        return user;
    }
}

