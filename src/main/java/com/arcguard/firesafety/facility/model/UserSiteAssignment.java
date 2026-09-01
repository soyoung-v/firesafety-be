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
public class UserSiteAssignment {

    private Long mappingId;
    private Long userId;
    private Long siteId;
    private String siteName;
    private String siteAddress;
    private LocalDateTime assignedAt;
}
