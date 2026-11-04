package com.collabsphere_first.messaging.repository;

import com.collabsphere_first.messaging.entity.MessageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, UUID> {

    List<MessageAttachment> findByMessageIdOrderByDisplayOrderAsc(UUID messageId);

    List<MessageAttachment> findByMessageIdIn(Collection<UUID> messageIds);

    List<MessageAttachment> findByMessageIdInOrderByDisplayOrderAsc(Collection<UUID> messageIds);

    void deleteByMessageId(UUID messageId);
}
