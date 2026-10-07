package se.lexicon.subscriptionapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import se.lexicon.subscriptionapi.domain.constant.ServiceType;

import java.math.BigDecimal;

public record PlanRequest(
        @NotBlank(message = "Plan name is required")
        String name,

        @NotNull(message = "Plan price is required")
        @Positive(message = "Price must be greater than zero")
        BigDecimal price,

        @NotNull(message = "Service type is required")
        ServiceType serviceType,

        @Positive(message = "Data limit must be positive if specified")
        Integer dataLimit,

        Boolean active,

        @NotNull(message = "Operator ID is required")
        Long operatorId
) {
}
