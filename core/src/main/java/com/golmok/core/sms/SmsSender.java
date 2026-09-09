package com.golmok.core.sms;

public interface SmsSender {

    void send(String phoneNumber, String message);
}
