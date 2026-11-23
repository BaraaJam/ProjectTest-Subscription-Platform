package se.lexicon.subscriptionapi.dto.request;

import jakarta.validation.constraints.NotNull;

public record ChangePlanRequest(
        @NotNull(message = "New Plan ID is required")
        Long newPlanId
) {
}