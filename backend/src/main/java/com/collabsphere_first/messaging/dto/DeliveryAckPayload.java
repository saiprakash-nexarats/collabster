package com.collabsphere_first.messaging.dto;

import java.util.UUID;

public record DeliveryAckPayload(
    UUID messageId
) {}
