package se.lexicon.subscriptionapi.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.lexicon.subscriptionapi.domain.constant.ServiceType;
import se.lexicon.subscriptionapi.domain.entity.Operator;
import se.lexicon.subscriptionapi.domain.entity.Plan;
import se.lexicon.subscriptionapi.dto.request.PlanRequest;
import se.lexicon.subscriptionapi.dto.response.PlanResponse;
import se.lexicon.subscriptionapi.exception.BusinessRuleException;
import se.lexicon.subscriptionapi.exception.ResourceNotFoundException;
import se.lexicon.subscriptionapi.mapper.PlanMapper;
import se.lexicon.subscriptionapi.repository.OperatorRepository;
import se.lexicon.subscriptionapi.repository.PlanRepository;
import se.lexicon.subscriptionapi.service.PlanService;

import java.util.List;

/**
 * Service implementation for managing subscription plans.
 * 
 * Engineering Pattern followed across methods:
 * 1. Guard / Input validation
 * 2. Fetch or Fail (database lookup with fast 404/exception)
 * 3. Business rule verification (uniqueness, status constraints)
 * 4. Entity mutation & persistence
 * 5. DTO projection via MapStruct
 */
@Service
@RequiredArgsConstructor
public class PlanServiceImpl implements PlanService {

    private final PlanRepository planRepository;
    private final OperatorRepository operatorRepository;
    private final PlanMapper planMapper;

    /**
     * Creates a new subscription plan.
     * Enforces uniqueness of plan name and verifies that the referenced operator exists.
     */
    @Override
    @Transactional
    public PlanResponse create(PlanRequest request) {
        // Step 1: Guard clause - fail fast if request is null
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }

        // Step 2: Business rule - ensure plan name is globally unique
        if (planRepository.existsByName(request.name())) {
            throw new BusinessRuleException("Plan name already exists: " + request.name());
        }

        // Step 3: Fetch parent entity - verify operator exists in database
        Operator operator = operatorRepository.findById(request.operatorId())
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found with id: " + request.operatorId()));

        // Step 4: Map DTO to entity and bind managed Operator relationship
        // (PlanMapper ignores operator to prevent unmanaged cascade or detached entity issues)
        Plan plan = planMapper.toEntity(request);
        plan.setOperator(operator);

        // Explicitly set active status if provided in request
        if (request.active() != null) {
            plan.setActive(request.active());
        }

        // Step 5: Persist managed entity and map to response DTO
        Plan savedPlan = planRepository.save(plan);
        return planMapper.toResponse(savedPlan);
    }

    /**
     * Updates an existing subscription plan.
     * Validates name uniqueness if modified, re-links operator if changed, and updates fields.
     */
    @Override
    @Transactional
    public PlanResponse update(Long id, PlanRequest request) {
        // Step 1: Guard clauses - ensure mandatory parameters are provided
        if (id == null) {
            throw new IllegalArgumentException("Plan ID cannot be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("Request body cannot be null");
        }

        // Step 2: Fetch existing entity or throw 404
        Plan existingPlan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + id));

        // Step 3: Business rule - if name has changed, ensure the new name is not already taken
        if (!existingPlan.getName().equalsIgnoreCase(request.name())
                && planRepository.existsByName(request.name())) {
            throw new BusinessRuleException("Plan name already exists: " + request.name());
        }

        // Step 4: Business rule - re-link operator if operator ID was changed
        if (!existingPlan.getOperator().getId().equals(request.operatorId())) {
            Operator newOperator = operatorRepository.findById(request.operatorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Operator not found with id: " + request.operatorId()));
            existingPlan.setOperator(newOperator);
        }

        // Step 5: Mutate fields on managed JPA entity
        existingPlan.setName(request.name());
        existingPlan.setPrice(request.price());
        existingPlan.setServiceType(request.serviceType());
        existingPlan.setDataLimit(request.dataLimit());
        if (request.active() != null) {
            existingPlan.setActive(request.active());
        }

        // Step 6: Save and return response DTO
        Plan updatedPlan = planRepository.save(existingPlan);
        return planMapper.toResponse(updatedPlan);
    }

    /**
     * Soft-deletes a plan by deactivating it.
     * Hard-deletion is avoided to maintain referential integrity for historical subscriptions.
     */
    @Override
    @Transactional
    public PlanResponse delete(Long id) {
        // Step 1: Guard clause
        if (id == null) {
            throw new IllegalArgumentException("Plan ID cannot be null");
        }

        // Step 2: Fetch existing entity or throw 404
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + id));

        // Step 3: Soft delete - deactivate so existing customer subscriptions stay valid
        plan.setActive(false);

        // Step 4: Persist and return response
        Plan savedPlan = planRepository.save(plan);
        return planMapper.toResponse(savedPlan);
    }

    /**
     * Retrieves all plans (both active and inactive).
     */
    @Override
    @Transactional(readOnly = true)
    public List<PlanResponse> findAll() {
        return planRepository.findAll()
                .stream()
                .map(planMapper::toResponse)
                .toList();
    }

    /**
     * Retrieves a single plan by its ID.
     */
    @Override
    @Transactional(readOnly = true)
    public PlanResponse findById(Long id) {
        // Step 1: Guard clause
        if (id == null) {
            throw new IllegalArgumentException("Plan ID cannot be null");
        }

        // Step 2: Fetch entity or throw 404
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + id));

        // Step 3: Map to response DTO
        return planMapper.toResponse(plan);
    }

    /**
     * Retrieves all active plans available for new subscriptions.
     */
    @Override
    @Transactional(readOnly = true)
    public List<PlanResponse> findAllActive() {
        return planRepository.findByActiveTrue()
                .stream()
                .map(planMapper::toResponse)
                .toList();
    }

    /**
     * Retrieves all active plans filtered by service type (e.g. MOBILE, INTERNET).
     */
    @Override
    @Transactional(readOnly = true)
    public List<PlanResponse> findAllActiveByServiceType(ServiceType serviceType) {
        // Step 1: Guard clause
        if (serviceType == null) {
            throw new IllegalArgumentException("ServiceType cannot be null");
        }

        // Step 2: Query repository and transform stream to DTO list
        return planRepository.findByActiveTrueAndServiceType(serviceType)
                .stream()
                .map(planMapper::toResponse)
                .toList();
    }

    /**
     * Retrieves all active plans belonging to a specific operator.
     */
    @Override
    @Transactional(readOnly = true)
    public List<PlanResponse> findAllActiveByOperatorId(Long operatorId) {
        // Step 1: Guard clause
        if (operatorId == null) {
            throw new IllegalArgumentException("Operator ID cannot be null");
        }

        // Step 2: Query repository and transform stream to DTO list
        return planRepository.findByOperatorIdAndActiveTrue(operatorId)
                .stream()
                .map(planMapper::toResponse)
                .toList();
    }
}
