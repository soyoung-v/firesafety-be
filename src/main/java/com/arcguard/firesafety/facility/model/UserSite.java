package com.arcguard.firesafety.facility.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserSite {

    private Long mappingId;
    private Long userId;
    private Long siteId;
    private LocalDateTime assignedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
