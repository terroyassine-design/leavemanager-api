package com.leavemanager.api.auth;

import com.leavemanager.api.user.UserResponse;

public record AuthResponse(String token, UserResponse user) {
}
