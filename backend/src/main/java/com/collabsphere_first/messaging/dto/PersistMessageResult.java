package com.collabsphere_first.messaging.dto;

import com.collabsphere_first.messaging.entity.DirectMessage;

public record PersistMessageResult(DirectMessage message, boolean isNew) {}
