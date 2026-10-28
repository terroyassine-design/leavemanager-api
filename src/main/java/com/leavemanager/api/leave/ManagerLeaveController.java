package com.leavemanager.api.leave;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/** Manager-only endpoints: access restricted to ROLE_MANAGER in SecurityConfig. */
@RestController
@RequestMapping("/api/manager/leaves")
public class ManagerLeaveController {

    private final LeaveService leaveService;

    public ManagerLeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @GetMapping
    public List<LeaveResponse> findAll(@RequestParam(required = false) LeaveStatus status) {
        return leaveService.findAll(status);
    }

    @PatchMapping("/{id}/approve")
    public LeaveResponse approve(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                 @Valid @RequestBody(required = false) DecisionRequest decision) {
        return leaveService.approve(LeaveController.userId(jwt), id, comment(decision));
    }

    @PatchMapping("/{id}/reject")
    public LeaveResponse reject(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                @Valid @RequestBody(required = false) DecisionRequest decision) {
        return leaveService.reject(LeaveController.userId(jwt), id, comment(decision));
    }

    private static String comment(DecisionRequest decision) {
        return decision != null ? decision.comment() : null;
    }
}
