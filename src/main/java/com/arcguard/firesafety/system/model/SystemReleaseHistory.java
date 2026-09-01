package com.arcguard.firesafety.system.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SystemReleaseHistory {

    private Long releaseId;
    private String version;
    private SystemReleaseType type;
    private String description;
    private String updatedBy;
    private LocalDateTime releasedAt;
}
