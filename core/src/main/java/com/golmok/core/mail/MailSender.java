package com.golmok.core.mail;

public interface MailSender {

    void send(String email, String subject, String body);
}
