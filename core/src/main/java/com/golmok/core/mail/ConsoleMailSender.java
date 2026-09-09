package com.golmok.core.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ConsoleMailSender implements MailSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleMailSender.class);

    @Override
    public void send(String email, String subject, String body) {
        log.info("[MAIL] to={} subject={} body={}", email, subject, body);
    }
}
