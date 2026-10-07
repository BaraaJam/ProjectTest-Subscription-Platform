package se.lexicon.subscriptionapi.dto.response;

import se.lexicon.subscriptionapi.domain.constant.ServiceType;
import se.lexicon.subscriptionapi.domain.constant.SubscriptionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SubscriptionResponse(
        Long id,
        Long customerId,
        Long planId,
        String planName,
        BigDecimal planPrice,
        ServiceType serviceType,
        String operatorName,
        SubscriptionStatus status,
        LocalDateTime startDate,
        LocalDateTime cancelledDate,
        LocalDateTime createdAt
) {
}