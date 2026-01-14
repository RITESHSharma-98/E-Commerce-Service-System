package com.ecommerce.store.services;

public interface OtpService {
    String generateOtp(String email);

    boolean verifyOtp(String email, String otpCode);
}
