package com.superdoc.api.service;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.IOException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private MailService service;

    private final String appPublicUrl = "https://example.com";
    private final String smtpFromAddress = "smtp@example.com";
    private final String defaultFrom = "no-reply@superdoc.local";

    @Test
    void sendPasswordResetEmail_withoutSendGrid_usesSMTP() {
        // Arrange
        String sendgridApiKey = ""; // Empty, so should fall back to SMTP
        service = new MailService(mailSender, appPublicUrl, smtpFromAddress, sendgridApiKey, defaultFrom);
        
        String toEmail = "user@example.com";
        String token = "test-token-123";

        // Act
        service.sendPasswordResetEmail(toEmail, token);

        // Assert
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        
        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getTo()).containsExactly(toEmail);
        assertThat(sentMessage.getFrom()).isEqualTo(smtpFromAddress);
        assertThat(sentMessage.getSubject()).contains("Reset your SuperDoc password");
        assertThat(sentMessage.getText()).contains(appPublicUrl + "/reset-password?token=" + token);
        assertThat(sentMessage.getText()).contains("valid for 1 hour");
    }

    @Test
    void sendPasswordResetEmail_withNullSMTPFrom_usesDefaultFrom() {
        // Arrange
        String sendgridApiKey = null;
        String nullSmtpFrom = null;
        service = new MailService(mailSender, appPublicUrl, nullSmtpFrom, sendgridApiKey, defaultFrom);
        
        String toEmail = "user@example.com";
        String token = "test-token-456";

        // Act
        service.sendPasswordResetEmail(toEmail, token);

        // Assert
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        
        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getFrom()).isEqualTo(defaultFrom);
    }

    @Test
    void sendPasswordResetEmail_withBlankSMTPFrom_usesDefaultFrom() {
        // Arrange
        String sendgridApiKey = "";
        String blankSmtpFrom = "   ";
        service = new MailService(mailSender, appPublicUrl, blankSmtpFrom, sendgridApiKey, defaultFrom);
        
        String toEmail = "user@example.com";
        String token = "test-token-789";

        // Act
        service.sendPasswordResetEmail(toEmail, token);

        // Assert
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        
        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getFrom()).isEqualTo(defaultFrom);
    }

    @Test
    void sendPasswordResetEmail_smtpFails_handlesException() {
        // Arrange
        String sendgridApiKey = "";
        service = new MailService(mailSender, appPublicUrl, smtpFromAddress, sendgridApiKey, defaultFrom);
        
        String toEmail = "user@example.com";
        String token = "test-token-error";
        
        doThrow(new RuntimeException("SMTP server unavailable"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        // Act - should not throw, should handle gracefully
        assertThatCode(() -> service.sendPasswordResetEmail(toEmail, token))
                .doesNotThrowAnyException();

        // Assert
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendPasswordResetEmail_containsCorrectContent() {
        // Arrange
        String sendgridApiKey = null;
        service = new MailService(mailSender, appPublicUrl, smtpFromAddress, sendgridApiKey, defaultFrom);
        
        String toEmail = "test@example.com";
        String token = "unique-token-xyz";

        // Act
        service.sendPasswordResetEmail(toEmail, token);

        // Assert
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        
        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getText())
                .contains("We received a request to reset your password")
                .contains(appPublicUrl + "/reset-password?token=" + token)
                .contains("valid for 1 hour")
                .contains("If you did not request this, you can ignore this email");
    }

    @Test
    void sendPasswordResetEmail_multipleTokens_sendsCorrectTokenEachTime() {
        // Arrange
        String sendgridApiKey = "";
        service = new MailService(mailSender, appPublicUrl, smtpFromAddress, sendgridApiKey, defaultFrom);
        
        String email1 = "user1@example.com";
        String token1 = "token-001";
        String email2 = "user2@example.com";
        String token2 = "token-002";

        // Act
        service.sendPasswordResetEmail(email1, token1);
        service.sendPasswordResetEmail(email2, token2);

        // Assert
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(2)).send(messageCaptor.capture());
        
        var messages = messageCaptor.getAllValues();
        assertThat(messages).hasSize(2);
        
        assertThat(messages.get(0).getTo()).containsExactly(email1);
        assertThat(messages.get(0).getText()).contains(token1);
        
        assertThat(messages.get(1).getTo()).containsExactly(email2);
        assertThat(messages.get(1).getText()).contains(token2);
    }

    @Test
    void sendPasswordResetEmail_withValidSMTPConfig_sendsEmail() {
        // Arrange
        String sendgridApiKey = "";
        String customSmtp = "custom@smtp.com";
        service = new MailService(mailSender, appPublicUrl, customSmtp, sendgridApiKey, defaultFrom);
        
        String toEmail = "recipient@example.com";
        String token = "reset-token";

        // Act
        service.sendPasswordResetEmail(toEmail, token);

        // Assert
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        
        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getFrom()).isEqualTo(customSmtp);
        assertThat(sentMessage.getTo()).containsExactly(toEmail);
        assertThat(sentMessage.getSubject()).isEqualTo("Reset your SuperDoc password");
    }
}

