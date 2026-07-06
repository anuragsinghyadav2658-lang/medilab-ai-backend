package com.medilab.backendlabreportassistant.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendCriticalAlert(String toEmail, String patientName, String alertMessage) {
        SimpleMailMessage message = new SimpleMailMessage();
        
        message.setTo(toEmail);
        message.setSubject("CRITICAL MEDICAL ALERT: Immediate Attention Required");
        
        // Email ki body properly format karke set karna
        String body = "Dear " + patientName + ",\n\n"
                    + "This is a critical medical alert generated based on your recent lab report analysis.\n\n"
                    + "ALERT DETAILS:\n"
                    + alertMessage + "\n\n"
                    + "Please consult your doctor or visit the nearest medical facility immediately.\n\n"
                    + "Regards,\n"
                    + "MediLab AI System";
                    
        message.setText(body);
        
        // Email bhejna
        mailSender.send(message);
    }
}
