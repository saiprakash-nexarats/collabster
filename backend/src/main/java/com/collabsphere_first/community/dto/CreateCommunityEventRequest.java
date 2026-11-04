package com.collabsphere_first.community.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.Instant;
@Data
public class CreateCommunityEventRequest {
    private java.util.UUID communityId;
    @NotBlank @Size(max = 200) private String title;
    @Size(max = 5000) private String description;
    @NotNull private Instant startsAt;
    @NotNull private Instant endsAt;
    @Size(max = 500) private String location;
}
