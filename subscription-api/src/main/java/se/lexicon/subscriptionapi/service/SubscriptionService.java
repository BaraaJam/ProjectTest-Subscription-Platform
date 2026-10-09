package se.lexicon.subscriptionapi.service;

import se.lexicon.subscriptionapi.dto.request.ChangePlanRequest;
import se.lexicon.subscriptionapi.dto.request.SubscriptionRequest;
import se.lexicon.subscriptionapi.dto.response.SubscriptionResponse;

import java.util.List;

public interface SubscriptionService {
    SubscriptionResponse subscribe(Long customerId, SubscriptionRequest request);
    SubscriptionResponse changePlan(Long subscriptionId, ChangePlanRequest request);
    SubscriptionResponse cancel(Long subscriptionId);
    SubscriptionResponse findById(Long id);
    List<SubscriptionResponse> findByCustomerId(Long customerId);
    List<SubscriptionResponse> findAll();
}
