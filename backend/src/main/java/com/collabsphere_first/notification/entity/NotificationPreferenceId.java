package com.collabsphere_first.notification.entity;

import com.collabsphere_first.notification.enums.NotificationCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreferenceId implements Serializable {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 40)
    private NotificationCategory category;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NotificationPreferenceId that = (NotificationPreferenceId) o;
        return Objects.equals(userId, that.userId) && category == that.category;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, category);
    }
}
