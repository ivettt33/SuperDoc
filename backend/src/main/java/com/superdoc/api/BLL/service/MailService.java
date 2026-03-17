package com.superdoc.api.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final String appPublicUrl;
    private final String smtpFromAddress;
    private final String sendgridApiKey;
    private final String defaultFrom;

    public MailService(JavaMailSender mailSender,
                       @Value("${app.publicUrl}") String appPublicUrl,
                       @Value("${spring.mail.username:}") String smtpFromAddress,
                       @Value("${sendgrid.apiKey:}") String sendgridApiKey,
                       @Value("${mail.from:no-reply@superdoc.local}") String defaultFrom) {
        this.mailSender = mailSender;
        this.appPublicUrl = appPublicUrl;
        this.smtpFromAddress = smtpFromAddress;
        this.sendgridApiKey = sendgridApiKey;
        this.defaultFrom = defaultFrom;
    }

    public void sendPasswordResetEmail(String toEmail, String token) {
        String resetUrl = appPublicUrl + "/reset-password?token=" + token;
        String subject = "Reset your SuperDoc password";
        String body = "We received a request to reset your password.\n\n" +
                "Click the link below to set a new password (valid for 1 hour):\n" + resetUrl + "\n\n" +
                "If you did not request this, you can ignore this email.";

        if (sendgridApiKey != null && !sendgridApiKey.isBlank()) {
            try {
                Email from = new Email(defaultFrom);
                Email to = new Email(toEmail);
                Content content = new Content("text/plain", body);
                Mail mail = new Mail(from, subject, to, content);
                SendGrid sg = new SendGrid(sendgridApiKey);
                Request request = new Request();
                request.setMethod(Method.POST);
                request.setEndpoint("mail/send");
                request.setBody(mail.build());
                Response response = sg.api(request);
                if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                    return;
                }
                log.warn("SendGrid failed (status {}): {}", response.getStatusCode(), response.getBody());
            } catch (Exception ex) {
                log.warn("SendGrid error: {}", ex.getMessage());
            }
        }

        // Fallback to SMTP if SendGrid not configured or failed
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            String from = (smtpFromAddress != null && !smtpFromAddress.isBlank()) ? smtpFromAddress : defaultFrom;
            message.setFrom(from);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception ex) {
            log.warn("SMTP send failed to {}: {}", toEmail, ex.getMessage());
        }
    }
}


