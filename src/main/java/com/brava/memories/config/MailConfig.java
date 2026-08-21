package com.brava.memories.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import java.util.Properties;

/** Only registers a JavaMailSender bean when MAIL_HOST is actually configured — see EmailService. */
@Configuration
public class MailConfig {
    @Bean
    public JavaMailSender mailSender(AppProperties props) {
        AppProperties.Mail mail = props.mail();
        if (mail.host() == null || mail.host().isBlank()) return null;
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(mail.host());
        sender.setPort(mail.port());
        boolean authenticated = mail.username() != null && !mail.username().isBlank();
        if (authenticated) { sender.setUsername(mail.username()); sender.setPassword(mail.password()); }
        Properties p = sender.getJavaMailProperties();
        p.put("mail.smtp.auth", String.valueOf(authenticated));
        p.put("mail.smtp.starttls.enable", "true");
        return sender;
    }
}
