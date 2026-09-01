package com.arcguard.firesafety.auth.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private Long userId;
    private String email;
    private String password;
    private String name;
    private String phone;
    private UserRole role;
    private UserAccountStatus accountStatus;
    private Long createdBy;
    private Long updatedBy;
    private Long deletedBy;
    private LocalDateTime restoredAt;
    private Long restoredBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
