package com.expensemate.expensemate_backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // Send OTP after transaction commits
    @TransactionalEventListener
    public void handleOtpEvent(OtpGeneratedEvent event) {
        sendOtpEmail(event.getEmail(), event.getOtp());
    }

    private void sendOtpEmail(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("ExpenseMate Password Reset OTP");
        message.setText("Your OTP is: " + otp + "\nValid for 10 minutes.");
        mailSender.send(message);
    }
}
