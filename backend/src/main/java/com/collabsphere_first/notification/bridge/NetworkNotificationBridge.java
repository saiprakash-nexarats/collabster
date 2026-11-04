package com.collabsphere_first.notification.bridge;

import com.collabsphere_first.network.event.ConnectionAcceptedEvent;
import com.collabsphere_first.network.event.ConnectionRequestedEvent;
import com.collabsphere_first.network.event.UserFollowedEvent;
import com.collabsphere_first.notification.dto.CreateNotificationCommand;
import com.collabsphere_first.notification.enums.NavigationTargetType;
import com.collabsphere_first.notification.enums.NotificationType;
import com.collabsphere_first.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NetworkNotificationBridge {

    private final NotificationService notificationService;

    @EventListener
    public void onUserFollowed(UserFollowedEvent event) {
        log.debug("Bridging UserFollowedEvent (followId={}, follower={}, following={})",
                event.getFollowId(), event.getFollowerId(), event.getFollowingId());

        String followOccurrenceId = event.getFollowId() != null
                ? event.getFollowId().toString()
                : String.valueOf(event.getFollowerId());

        CreateNotificationCommand cmd = CreateNotificationCommand.builder()
                .recipientUserId(event.getFollowingId())
                .actorUserId(event.getFollowerId())
                .type(NotificationType.NEW_FOLLOW)
                .sourceType("USER_FOLLOW")
                .sourceId(followOccurrenceId)
                .eventKey("FOLLOW:" + followOccurrenceId + ":" + event.getFollowingId())
                .titleKey("notification.follow.title")
                .bodyKey("notification.follow.body")
                .targetType(NavigationTargetType.OPEN_PROFILE)
                .targetId(String.valueOf(event.getFollowerId()))
                .payload(Map.of("followerId", event.getFollowerId()))
                .build();

        notificationService.createNotification(cmd);
    }

    @EventListener
    public void onConnectionRequested(ConnectionRequestedEvent event) {
        log.debug("Bridging ConnectionRequestedEvent (requestId={}, requester={}, addressee={}, eventId={})",
                event.getRequestId(), event.getRequesterId(), event.getAddresseeId(), event.getEventId());

        String requestOccurrenceId = event.getEventId() != null
                ? event.getEventId().toString()
                : event.getRequestId().toString();

        CreateNotificationCommand cmd = CreateNotificationCommand.builder()
                .recipientUserId(event.getAddresseeId())
                .actorUserId(event.getRequesterId())
                .type(NotificationType.CONNECTION_REQUESTED)
                .sourceType("CONNECTION_REQUEST")
                .sourceId(event.getRequestId().toString())
                .eventKey("CONNECTION_REQUEST:" + requestOccurrenceId + ":" + event.getAddresseeId())
                .titleKey("notification.connection_request.title")
                .bodyKey("notification.connection_request.body")
                .targetType(NavigationTargetType.OPEN_CONNECTION_REQUEST)
                .targetId(event.getRequestId().toString())
                .payload(Map.of(
                        "requestId", event.getRequestId().toString(),
                        "requesterId", event.getRequesterId()
                ))
                .build();

        notificationService.createNotification(cmd);
    }

    @EventListener
    public void onConnectionAccepted(ConnectionAcceptedEvent event) {
        log.debug("Bridging ConnectionAcceptedEvent (requestId={}, requester={}, addressee={}, eventId={})",
                event.getRequestId(), event.getRequesterId(), event.getAddresseeId(), event.getEventId());

        String acceptedOccurrenceId = event.getEventId() != null
                ? event.getEventId().toString()
                : event.getRequestId().toString();

        CreateNotificationCommand cmd = CreateNotificationCommand.builder()
                .recipientUserId(event.getRequesterId())
                .actorUserId(event.getAddresseeId())
                .type(NotificationType.CONNECTION_ACCEPTED)
                .sourceType("CONNECTION_REQUEST")
                .sourceId(event.getRequestId().toString())
                .eventKey("CONNECTION_ACCEPTED:" + acceptedOccurrenceId + ":" + event.getRequesterId())
                .titleKey("notification.connection_accepted.title")
                .bodyKey("notification.connection_accepted.body")
                .targetType(NavigationTargetType.OPEN_PROFILE)
                .targetId(String.valueOf(event.getAddresseeId()))
                .payload(Map.of(
                        "requestId", event.getRequestId().toString(),
                        "accepterId", event.getAddresseeId()
                ))
                .build();

        notificationService.createNotification(cmd);
    }
}
