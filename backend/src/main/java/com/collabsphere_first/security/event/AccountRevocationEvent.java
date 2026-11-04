package com.collabsphere_first.security.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Event broadcast across cluster nodes when an account-wide session revocation occurs
 * (logout-all, account deactivation, account deletion, admin suspension).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountRevocationEvent implements Serializable {
    private Long userId;
    private Long newAuthVersion;
    private String reason;
    private long timestamp;
}
