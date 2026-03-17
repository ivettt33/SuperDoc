package com.superdoc.api.controller;

import com.superdoc.api.enumerate.Role;
import com.superdoc.api.persistence.entities.UserEntity;
import com.superdoc.api.persistence.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static com.superdoc.api.controller.TestAuthHelper.withEmail;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MeController.class)
@Import(TestSecurityConfig.class)
class MeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @Test
    void shouldReturnUserDataWhenAuthenticatedAsDoctor() throws Exception {
        // Arrange
        String email = "doctor@example.com";
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setEmail(email);
        user.setRole(Role.DOCTOR);
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // Act & Assert
        mockMvc.perform(get("/me")
                        .with(withEmail(email))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("DOCTOR"));

        verify(userRepository).findByEmail(email);
    }

    @Test
    void shouldReturnUserDataWhenAuthenticatedAsPatient() throws Exception {
        // Arrange
        String email = "patient@example.com";
        UserEntity user = new UserEntity();
        user.setId(2L);
        user.setEmail(email);
        user.setRole(Role.PATIENT);
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // Act & Assert
        mockMvc.perform(get("/me")
                        .with(withEmail(email))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("PATIENT"));

        verify(userRepository).findByEmail(email);
    }

}

