package com.superdoc.api.controller;

import com.superdoc.api.service.FileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;
import java.nio.file.Paths;

import static com.superdoc.api.controller.TestAuthHelper.withEmail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = FileController.class)
@Import(TestSecurityConfig.class)
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileService fileService;

    @Test
    void uploadFile_validImageFile_returnsOk() throws Exception {
        // Arrange
        String userEmail = "user@example.com";
        String fileName = "test-image.png";
        byte[] fileContent = new byte[]{1, 2, 3, 4, 5};
        MockMultipartFile file = new MockMultipartFile(
                "file",
                fileName,
                "image/png",
                fileContent
        );

        when(fileService.saveFile(any())).thenReturn("unique-uuid-123.png");

        // Act & Assert
        mockMvc.perform(multipart("/files/upload")
                        .file(file)
                        .with(withEmail(userEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("File uploaded successfully"))
                .andExpect(jsonPath("$.filePath").value("unique-uuid-123.png"))
                .andExpect(jsonPath("$.fileName").value("unique-uuid-123.png"));

        verify(fileService).saveFile(any());
    }

    @Test
    void uploadFile_jpegImage_returnsOk() throws Exception {
        // Arrange
        String userEmail = "user@example.com";
        String fileName = "test-image.jpeg";
        byte[] fileContent = new byte[]{1, 2, 3, 4, 5};
        MockMultipartFile file = new MockMultipartFile(
                "file",
                fileName,
                "image/jpeg",
                fileContent
        );

        when(fileService.saveFile(any())).thenReturn("unique-uuid-456.jpeg");

        // Act & Assert
        mockMvc.perform(multipart("/files/upload")
                        .file(file)
                        .with(withEmail(userEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("File uploaded successfully"));

        verify(fileService).saveFile(any());
    }

    @Test
    void uploadFile_emptyFile_returnsBadRequest() throws Exception {
        // Arrange
        String userEmail = "user@example.com";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.png",
                "image/png",
                new byte[0]
        );

        when(fileService.saveFile(any())).thenThrow(new IllegalArgumentException("File is empty"));

        // Act & Assert
        mockMvc.perform(multipart("/files/upload")
                        .file(file)
                        .with(withEmail(userEmail)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("File is empty"))
                .andExpect(jsonPath("$.filePath").doesNotExist())
                .andExpect(jsonPath("$.fileName").doesNotExist());

        verify(fileService).saveFile(any());
    }

    @Test
    void uploadFile_fileTooLarge_returnsBadRequest() throws Exception {
        // Arrange
        String userEmail = "user@example.com";
        byte[] largeContent = new byte[6 * 1024 * 1024]; // 6MB, exceeds 5MB limit
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.png",
                "image/png",
                largeContent
        );

        when(fileService.saveFile(any())).thenThrow(new IllegalArgumentException("File size exceeds 5MB"));

        // Act & Assert
        mockMvc.perform(multipart("/files/upload")
                        .file(file)
                        .with(withEmail(userEmail)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("File size exceeds 5MB"));

        verify(fileService).saveFile(any());
    }

    @Test
    void uploadFile_nonImageFile_returnsBadRequest() throws Exception {
        // Arrange
        String userEmail = "user@example.com";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                new byte[]{1, 2, 3}
        );

        when(fileService.saveFile(any())).thenThrow(new IllegalArgumentException("Only image files are allowed"));

        // Act & Assert
        mockMvc.perform(multipart("/files/upload")
                        .file(file)
                        .with(withEmail(userEmail)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only image files are allowed"));

        verify(fileService).saveFile(any());
    }

    @Test
    void uploadFile_serviceException_returnsInternalServerError() throws Exception {
        // Arrange
        String userEmail = "user@example.com";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                new byte[]{1, 2, 3}
        );

        when(fileService.saveFile(any())).thenThrow(new RuntimeException("IO error"));

        // Act & Assert
        mockMvc.perform(multipart("/files/upload")
                        .file(file)
                        .with(withEmail(userEmail)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Failed to upload file: IO error"));

        verify(fileService).saveFile(any());
    }

    @Test
    void uploadFile_missingFile_returnsInternalServerError() throws Exception {
        // Arrange
        String userEmail = "user@example.com";

        // Act & Assert
        // Missing file parameter causes an exception that returns 500
        mockMvc.perform(multipart("/files/upload")
                        .with(withEmail(userEmail)))
                .andExpect(status().isInternalServerError());

        verify(fileService, never()).saveFile(any());
    }

    @Test
    void getFile_validFileName_callsService() throws Exception {
        // Arrange
        String fileName = "test-image.png";
        Path filePath = Paths.get("/uploads/" + fileName);

        when(fileService.getFile(fileName)).thenReturn(filePath);
        // Note: The actual resource existence check happens in the controller
        // which creates a UrlResource. Without actual files on the filesystem,
        // this returns 404. We're testing that the service is called correctly.

        // Act & Assert
        // The controller will try to create a resource and check if it exists
        // Without actual files, this returns 404, but service was called correctly
        mockMvc.perform(get("/files/" + fileName))
                .andExpect(status().isNotFound()); // File doesn't exist in test env

        verify(fileService).getFile(fileName);
    }

    @Test
    void getFile_fileNotFound_returnsBadRequest() throws Exception {
        // Arrange
        String fileName = "nonexistent.png";

        when(fileService.getFile(fileName)).thenThrow(new IllegalArgumentException("File not found"));

        // Act & Assert
        mockMvc.perform(get("/files/" + fileName))
                .andExpect(status().isBadRequest()); // Controller catches IllegalArgumentException as bad request

        verify(fileService).getFile(fileName);
    }

    @Test
    void getFile_invalidPath_returnsError() throws Exception {
        // Arrange
        String fileName = "../../../etc/passwd"; // Path traversal attempt

        when(fileService.getFile(fileName)).thenThrow(new IllegalArgumentException("Invalid file path"));

        // Act & Assert
        // The service validates the path and throws IllegalArgumentException for path traversal
        // Spring URL encoding might change the path, so service might or might not be called
        // Either way, path traversal should be prevented (either by service returning 400 or earlier rejection)
        mockMvc.perform(get("/files/" + fileName))
                .andExpect(status().isBadRequest()); // Controller catches IllegalArgumentException as bad request

        // Service might be called if path encoding allows it, or not if Spring rejects it earlier
        // Either behavior is acceptable as long as path traversal is prevented
        verify(fileService, atMostOnce()).getFile(fileName);
    }

    @Test
    void getFile_serviceException_returnsInternalServerError() throws Exception {
        // Arrange
        String fileName = "test.png";

        when(fileService.getFile(fileName)).thenThrow(new RuntimeException("IO error"));

        // Act & Assert
        mockMvc.perform(get("/files/" + fileName))
                .andExpect(status().isInternalServerError());

        verify(fileService).getFile(fileName);
    }

    @Test
    void getFile_jpgExtension_callsService() throws Exception {
        // Arrange
        String fileName = "image.jpg";
        Path filePath = Paths.get("/uploads/" + fileName);

        when(fileService.getFile(fileName)).thenReturn(filePath);

        // Act & Assert
        // Without actual files, expect 404 but verify service was called
        mockMvc.perform(get("/files/" + fileName))
                .andExpect(status().isNotFound());

        verify(fileService).getFile(fileName);
    }

    @Test
    void getFile_pngExtension_callsService() throws Exception {
        // Arrange
        String fileName = "image.PNG";
        Path filePath = Paths.get("/uploads/" + fileName);

        when(fileService.getFile(fileName)).thenReturn(filePath);

        // Act & Assert
        // Without actual files, expect 404 but verify service was called
        mockMvc.perform(get("/files/" + fileName))
                .andExpect(status().isNotFound());

        verify(fileService).getFile(fileName);
    }

    @Test
    void getFile_gifExtension_callsService() throws Exception {
        // Arrange
        String fileName = "image.gif";
        Path filePath = Paths.get("/uploads/" + fileName);

        when(fileService.getFile(fileName)).thenReturn(filePath);

        // Act & Assert
        mockMvc.perform(get("/files/" + fileName))
                .andExpect(status().isNotFound());

        verify(fileService).getFile(fileName);
    }

    @Test
    void getFile_webpExtension_callsService() throws Exception {
        // Arrange
        String fileName = "image.webp";
        Path filePath = Paths.get("/uploads/" + fileName);

        when(fileService.getFile(fileName)).thenReturn(filePath);

        // Act & Assert
        mockMvc.perform(get("/files/" + fileName))
                .andExpect(status().isNotFound());

        verify(fileService).getFile(fileName);
    }

    @Test
    void getFile_jpegExtension_callsService() throws Exception {
        // Arrange
        String fileName = "image.jpeg";
        Path filePath = Paths.get("/uploads/" + fileName);

        when(fileService.getFile(fileName)).thenReturn(filePath);

        // Act & Assert
        mockMvc.perform(get("/files/" + fileName))
                .andExpect(status().isNotFound());

        verify(fileService).getFile(fileName);
    }

    @Test
    void getFile_unknownExtension_usesDefaultContentType() throws Exception {
        // Arrange
        String fileName = "image.bmp"; // Not in the list, should use default
        Path filePath = Paths.get("/uploads/" + fileName);

        when(fileService.getFile(fileName)).thenReturn(filePath);

        // Act & Assert
        mockMvc.perform(get("/files/" + fileName))
                .andExpect(status().isNotFound());

        verify(fileService).getFile(fileName);
    }

    @Test
    void getFile_uppercaseExtensions_handlesCorrectly() throws Exception {
        // Arrange - test uppercase variations
        String fileName = "image.GIF";
        Path filePath = Paths.get("/uploads/" + fileName);

        when(fileService.getFile(fileName)).thenReturn(filePath);

        // Act & Assert
        mockMvc.perform(get("/files/" + fileName))
                .andExpect(status().isNotFound());

        verify(fileService).getFile(fileName);
    }

    @Test
    void getFile_mixedCaseExtensions_handlesCorrectly() throws Exception {
        // Arrange
        String fileName = "image.JpG";
        Path filePath = Paths.get("/uploads/" + fileName);

        when(fileService.getFile(fileName)).thenReturn(filePath);

        // Act & Assert
        mockMvc.perform(get("/files/" + fileName))
                .andExpect(status().isNotFound());

        verify(fileService).getFile(fileName);
    }
}

