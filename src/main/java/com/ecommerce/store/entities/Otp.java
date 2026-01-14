package com.ecommerce.store.entities;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "otps")
public class Otp {

    @Id
    private String email;

    private String otpCode;

    private LocalDateTime expiryTime;
}
