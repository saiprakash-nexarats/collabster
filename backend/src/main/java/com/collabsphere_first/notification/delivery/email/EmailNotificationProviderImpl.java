package com.collabsphere_first.notification.delivery.email;

import com.collabsphere_first.notification.config.NotificationProperties;
import com.collabsphere_first.notification.entity.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationProviderImpl implements EmailNotificationProvider {

    private final JavaMailSender mailSender;
    private final NotificationProperties properties;

    @Override
    public boolean isEnabled() {
        return properties.getEmail().isEnabled();
    }

    @Override
    public void sendNotificationEmail(String recipientEmail, String recipientName, Notification notification) {
        if (!isEnabled()) {
            log.info("Email notification delivery skipped: email provider disabled (notificationId={})",
                    notification.getId());
            return;
        }

        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new IllegalArgumentException("Recipient email cannot be empty");
        }

        String subject = "CollabSphere: New Notification";
        String bodyText = "Hello " + (recipientName != null ? recipientName : "there") + ",\n\n" +
                "You have a new " + notification.getCategory() + " notification on CollabSphere:\n\n" +
                notification.getType().name() + "\n\n" +
                "Log in to CollabSphere to view details.\n\n" +
                "Regards,\nCollabSphere Notifications Team";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getEmail().getFromAddress());
        message.setTo(recipientEmail);
        message.setSubject(subject);
        message.setText(bodyText);

        mailSender.send(message);
        log.info("Dispatched notification email (notificationId={}, category={})",
                notification.getId(), notification.getCategory());
    }
}
