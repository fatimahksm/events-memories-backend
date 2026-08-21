package com.brava.memories.auth;

import com.brava.memories.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/** Sends password-reset emails. Logs (rather than sends) when no SMTP server is configured,
 *  so local development and testing still work without real mail credentials. */
@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final AppProperties props;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider, AppProperties props) {
        this.mailSenderProvider = mailSenderProvider; this.props = props;
    }

    public void sendPasswordReset(String toEmail, String resetLink) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            log.warn("MAIL_HOST is not configured; would have sent a password reset email to {} with link: {}", toEmail, resetLink);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setFrom(props.mail().fromAddress());
        message.setSubject("Reset your Brava Event Memories password");
        message.setText("We received a request to reset your password.\n\n"
                + "Reset it here: " + resetLink + "\n\n"
                + "This link expires in one hour. If you didn't request this, you can safely ignore this email.");
        sender.send(message);
    }
}
