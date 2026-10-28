package com.leavemanager.api.leave;

import jakarta.validation.constraints.Size;

/** Optional comment left by the manager when approving or rejecting. */
public record DecisionRequest(@Size(max = 500) String comment) {
}
