package com.leavemanager.api.user;

public record UserResponse(Long id, String fullName, String email, Role role, int leaveBalance, boolean demo) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole(),
                user.getLeaveBalance(), user.isDemo());
    }
}
