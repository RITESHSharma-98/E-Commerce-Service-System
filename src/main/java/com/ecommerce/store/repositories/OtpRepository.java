package com.ecommerce.store.repositories;

import com.ecommerce.store.entities.Otp;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpRepository extends JpaRepository<Otp, String> {
}
