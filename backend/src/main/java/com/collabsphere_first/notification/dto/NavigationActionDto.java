package com.collabsphere_first.notification.dto;

import com.collabsphere_first.notification.enums.NavigationTargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NavigationActionDto {
    private NavigationTargetType targetType;
    private String targetId;
}
