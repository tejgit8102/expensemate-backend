package com.expensemate.expensemate_backend.service;


import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class OtpGeneratedEvent extends ApplicationEvent {
    private final String email;
    private final String otp;

    public OtpGeneratedEvent(Object source, String email, String otp) {
        super(source);
        this.email = email;
        this.otp = otp;
    }
}
