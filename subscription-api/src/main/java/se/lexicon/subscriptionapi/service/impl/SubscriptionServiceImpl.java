package se.lexicon.subscriptionapi.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.lexicon.subscriptionapi.domain.constant.SubscriptionStatus;
import se.lexicon.subscriptionapi.domain.entity.Customer;
import se.lexicon.subscriptionapi.domain.entity.Plan;
import se.lexicon.subscriptionapi.domain.entity.Subscription;
import se.lexicon.subscriptionapi.dto.request.ChangePlanRequest;
import se.lexicon.subscriptionapi.dto.request.SubscriptionRequest;
import se.lexicon.subscriptionapi.dto.response.SubscriptionResponse;
import se.lexicon.subscriptionapi.exception.BusinessRuleException;
import se.lexicon.subscriptionapi.exception.ResourceNotFoundException;
import se.lexicon.subscriptionapi.mapper.SubscriptionMapper;
import se.lexicon.subscriptionapi.repository.CustomerRepository;
import se.lexicon.subscriptionapi.repository.PlanRepository;
import se.lexicon.subscriptionapi.repository.SubscriptionRepository;
import se.lexicon.subscriptionapi.service.SubscriptionService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service implementation for managing customer subscriptions.
 *
 * Engineering Pattern followed across methods:
 * 1. Guard / Input validation (fail fast on nulls)
 * 2. Fetch or Fail (database lookup with fast 404/exception)
 * 3. Business rule verification (plan status, duplicate service types, same operator/type on change)
 * 4. Entity mutation & persistence (saving managed entities)
 * 5. DTO projection via MapStruct (flattening nested entity properties)
 */
@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final CustomerRepository customerRepository;
    private final PlanRepository planRepository;
    private final SubscriptionMapper subscriptionMapper;

    /**
     * Subscribes a customer to a plan.
     * Enforces active plan requirement and at most one active subscription per service type.
     */
    @Override
    @Transactional
    public SubscriptionResponse subscribe(Long customerId, SubscriptionRequest request) {
        // Step 1: Guard clause - fail fast if inputs are null
        if (customerId == null) {
            throw new IllegalArgumentException("customerId is null");
        }
        if (request == null) {
            throw new IllegalArgumentException("request is null");
        }

        // Step 2: Fetch parent entities or throw 404
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        Plan plan = planRepository.findById(request.planId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + request.planId()));

        // Step 3: Business rule - plan must be active to accept subscriptions
        if (!plan.isActive()) {
            throw new BusinessRuleException("Cannot subscribe to an inactive plan");
        }

        // Step 4: Business rule - at most one active subscription per service type (Internet / Mobile)
        boolean alreadySubscribed = subscriptionRepository.existsByCustomerIdAndPlan_ServiceTypeAndStatus(
                customerId,
                plan.getServiceType(),
                SubscriptionStatus.ACTIVE
        );
        if (alreadySubscribed) {
            throw new BusinessRuleException("Customer already has an active subscription for service type: " + plan.getServiceType());
        }

        // Step 5: Construct managed entity with initial ACTIVE state and timestamp
        Subscription subscription = new Subscription();
        subscription.setCustomer(customer);
        subscription.setPlan(plan);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartDate(LocalDateTime.now());

        // Step 6: Persist entity and project to flattened response DTO
        Subscription savedSubscription = subscriptionRepository.save(subscription);
        return subscriptionMapper.toResponse(savedSubscription);
    }

    /**
     * Changes the plan of an existing active subscription.
     * Enforces: active status, target plan active, same operator, and same service type.
     */
    @Override
    @Transactional
    public SubscriptionResponse changePlan(Long subscriptionId, ChangePlanRequest request) {
        // Step 1: Guard clauses - ensure valid identifiers and payload
        if (subscriptionId == null) {
            throw new IllegalArgumentException("subscriptionId is null");
        }
        if (request == null) {
            throw new IllegalArgumentException("request is null");
        }

        // Step 2: Fetch existing subscription and target plan or throw 404
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found with id: " + subscriptionId));

        Plan newPlan = planRepository.findById(request.newPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + request.newPlanId()));

        // Step 3: Business rule - cannot change plan on cancelled or inactive subscriptions
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new BusinessRuleException("Cannot change plan on a cancelled or inactive subscription");
        }

        // Step 4: Business rule - target plan must be active
        if (!newPlan.isActive()) {
            throw new BusinessRuleException("Cannot change to an inactive plan");
        }

        // Step 5: Business rule - plan change allowed only within the same operator
        if (!subscription.getPlan().getOperator().getId().equals(newPlan.getOperator().getId())) {
            throw new BusinessRuleException("Plan change is allowed only within the same operator");
        }

        // Step 6: Business rule - plan change allowed only within the same service type
        if (!subscription.getPlan().getServiceType().equals(newPlan.getServiceType())) {
            throw new BusinessRuleException("Plan change is allowed only within the same service type");
        }

        // Step 7: Update relationship on managed entity, persist, and map to DTO
        subscription.setPlan(newPlan);
        Subscription updatedSubscription = subscriptionRepository.save(subscription);
        return subscriptionMapper.toResponse(updatedSubscription);
    }

    /**
     * Cancels an active subscription.
     * Updates status to CANCELLED, sets cancellation timestamp, and prevents repeat cancellation.
     */
    @Override
    @Transactional
    public SubscriptionResponse cancel(Long subscriptionId) {
        // Step 1: Guard clause
        if (subscriptionId == null) {
            throw new IllegalArgumentException("subscriptionId is null");
        }

        // Step 2: Fetch existing subscription or throw 404
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found with id: " + subscriptionId));

        // Step 3: Business rule - reject if already cancelled
        if (subscription.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot cancel subscription, it is already cancelled");
        }

        // Step 4: Mutate status and store cancellation timestamp
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setCancelledDate(LocalDateTime.now());

        // Step 5: Persist managed entity and map to response DTO
        Subscription savedSubscription = subscriptionRepository.save(subscription);
        return subscriptionMapper.toResponse(savedSubscription);
    }

    /**
     * Retrieves a single subscription by primary key.
     */
    @Override
    @Transactional(readOnly = true)
    public SubscriptionResponse findById(Long id) {
        // Step 1: Guard clause
        if (id == null) {
            throw new IllegalArgumentException("id is null");
        }

        // Step 2: Fetch or throw 404
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found with id: " + id));

        // Step 3: Map entity to response DTO
        return subscriptionMapper.toResponse(subscription);
    }

    /**
     * Retrieves all subscriptions belonging to a specific customer.
     */
    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionResponse> findByCustomerId(Long customerId) {
        // Step 1: Guard clause
        if (customerId == null) {
            throw new IllegalArgumentException("customerId is null");
        }

        // Step 2: Query repository and stream entities to response DTOs
        return subscriptionRepository.findByCustomerId(customerId)
                .stream()
                .map(subscriptionMapper::toResponse)
                .toList();
    }

    /**
     * Retrieves all subscriptions in the system (Admin operation).
     */
    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionResponse> findAll() {
        // Stream all subscriptions to response DTOs
        return subscriptionRepository.findAll()
                .stream()
                .map(subscriptionMapper::toResponse)
                .toList();
    }
}
