package com.collabsphere_first.workspace.controller;

import com.collabsphere_first.workspace.dto.SelectWorkspaceRequest;
import com.collabsphere_first.workspace.dto.WorkspaceOnboardingStatusResponse;
import com.collabsphere_first.workspace.dto.WorkspaceResponse;
import com.collabsphere_first.workspace.service.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @GetMapping("/test")
public String test() {
    return "Workspace Controller Working";
}

   @PostMapping("/primary")
public WorkspaceResponse selectPrimaryWorkspace(
        @Valid @RequestBody SelectWorkspaceRequest request) {

    System.out.println(">>> Workspace API HIT <<<");

    return workspaceService.selectPrimaryWorkspace(request);
}

    @PostMapping("/secondary")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkspaceResponse selectSecondaryWorkspace(
            @Valid @RequestBody SelectWorkspaceRequest request) {

        return workspaceService.selectSecondaryWorkspace(request);
    }

    @PostMapping("/secondary/skip")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void skipSecondaryWorkspace() {

        workspaceService.skipSecondaryWorkspace();
    }

    @GetMapping("/me")
    public List<WorkspaceResponse> getMyWorkspaces() {

        return workspaceService.getMyWorkspaces();
    }

    @GetMapping("/onboarding-status")
public WorkspaceOnboardingStatusResponse getOnboardingStatus() {
    return workspaceService.getOnboardingStatus();
}
}