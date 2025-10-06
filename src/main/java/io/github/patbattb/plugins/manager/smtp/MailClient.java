package io.github.patbattb.plugins.manager.smtp;

import org.simplejavamail.MailException;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;

import java.util.List;

public class MailClient {

    private final Mailer mailer;
    private final Sender sender;
    private final List<String> recipients;

    public MailClient(String host, int port, String username, String password, boolean useSSL,
                      Sender sender, List<String> recipients) {
        mailer = MailerBuilder.withSMTPServer(host, port, username, password)
                .withTransportStrategy(useSSL ? TransportStrategy.SMTPS : TransportStrategy.SMTP)
                .withSessionTimeout(10000)
                .buildMailer();
        this.sender = sender;
        this.recipients = recipients;
    }

    public void sendEmail(String subject, String body) throws MailException {
        Email email = EmailBuilder.startingBlank()
                                .from(sender.name(), sender.email())
                                .to(String.join(",", recipients))
                                .withSubject(subject)
                                .appendText(body)
                                .buildEmail();
        mailer.sendMail(email);
    }
}
