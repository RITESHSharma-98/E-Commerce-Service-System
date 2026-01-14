package com.ecommerce.store.services.impl;

import com.ecommerce.store.entities.Otp;
import com.ecommerce.store.repositories.OtpRepository;
import com.ecommerce.store.services.OtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class OtpServiceImpl implements OtpService {

    @Autowired
    private OtpRepository otpRepository;

    @Override
    public String generateOtp(String email) {
        Optional<Otp> existingOtp = otpRepository.findById(email);

        // Check if a valid (not expired) OTP already exists
        if (existingOtp.isPresent() && existingOtp.get().getExpiryTime().isAfter(LocalDateTime.now())) {
            return existingOtp.get().getOtpCode();
        }

        // Generate new 6-digit OTP
        String otpCode = String.format("%06d", new Random().nextInt(999999));

        Otp otp = Otp.builder()
                .email(email)
                .otpCode(otpCode)
                .expiryTime(LocalDateTime.now().plusMinutes(5)) // Valid for 5 minutes
                .build();

        otpRepository.save(otp);
        return otpCode;
    }

    @Override
    public boolean verifyOtp(String email, String otpCode) {
        Optional<Otp> otpOptional = otpRepository.findById(email);

        if (otpOptional.isPresent()) {
            Otp otp = otpOptional.get();
            if (otp.getOtpCode().equals(otpCode) && otp.getExpiryTime().isAfter(LocalDateTime.now())) {
                // Delete OTP after successful verification
                otpRepository.delete(otp);
                return true;
            }
        }
        return false;
    }
}
