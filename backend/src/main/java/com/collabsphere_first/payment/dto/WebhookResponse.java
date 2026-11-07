package com.collabsphere_first.payment.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookResponse {
    private String status;
    private String eventId;
    private Boolean processed;
}

