package com.collabsphere_first.workspace.entity;

import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.workspace.enums.WorkspacePriority;
import com.collabsphere_first.workspace.enums.WorkspaceStatus;
import com.collabsphere_first.workspace.enums.WorkspaceType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_workspaces",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "workspace_type"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserWorkspace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "workspace_type", nullable = false, length = 50)
    private WorkspaceType workspaceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkspaceStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkspacePriority priority;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();


        if (status == null) {
            status = WorkspaceStatus.PENDING;
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}