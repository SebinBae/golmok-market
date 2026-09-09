package com.golmok.core.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ConsoleSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleSmsSender.class);

    @Override
    public void send(String phoneNumber, String message) {
        log.info("[SMS] to={} message={}", phoneNumber, message);
    }
}
