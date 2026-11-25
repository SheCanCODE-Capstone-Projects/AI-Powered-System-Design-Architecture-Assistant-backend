package org.example.aipowereddesigntbackend.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Sends transactional emails through Brevo. Delivery failures are logged and
 * swallowed rather than propagated, since a missed notification email must
 * never roll back the invitation that triggered it.
 */
@Service
@Slf4j
public class EmailService {

    private static final String BREVO_SEND_URL = "https://api.brevo.com/v3/smtp/email";

    private final RestClient restClient = RestClient.create();

    @Value("${BREVO_API}")
    private String apiKey;

    @Value("${BREVO_MAIL}")
    private String senderEmail;

    @Value("${BREVO_SENDER_NAME}")
    private String senderName;

    public void sendProjectInvitationEmail(
            String recipientEmail, String projectName, String inviterName, String acceptUrl
    ) {
        try {
            Map<String, Object> payload = Map.of(
                    "sender", Map.of("name", senderName, "email", senderEmail),
                    "to", List.of(Map.of("email", recipientEmail)),
                    "subject", inviterName + " invited you to collaborate on \"" + projectName + "\"",
                    "htmlContent", buildInvitationHtml(projectName, inviterName, acceptUrl)
            );

            restClient.post()
                    .uri(BREVO_SEND_URL)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("api-key", apiKey)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Failed to send project invitation email to {}: {}", recipientEmail, ex.getMessage());
        }
    }

    private String buildInvitationHtml(String projectName, String inviterName, String acceptUrl) {
        return """
                <p>%s invited you to collaborate on <strong>%s</strong> on ArchPilot.</p>
                <p><a href="%s">Accept the invitation</a></p>
                <p>If you don't have an ArchPilot account yet, log in with Google or GitHub using this \
                email address and the invitation will be waiting for you.</p>
                """.formatted(inviterName, projectName, acceptUrl);
    }
}