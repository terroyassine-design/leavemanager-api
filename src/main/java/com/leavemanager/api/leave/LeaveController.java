package com.leavemanager.api.leave;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/** Endpoints for the logged-in employee's own leave requests. */
@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @GetMapping("/me")
    public List<LeaveResponse> myLeaves(@AuthenticationPrincipal Jwt jwt) {
        return leaveService.findMine(userId(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateLeaveRequest request) {
        return leaveService.create(userId(jwt), request);
    }

    @PatchMapping("/{id}/cancel")
    public LeaveResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return leaveService.cancel(userId(jwt), id);
    }

    /** The JWT subject holds the user id (see JwtService). */
    static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
