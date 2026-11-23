package se.lexicon.subscriptionapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import se.lexicon.subscriptionapi.dto.request.ChangePlanRequest;
import se.lexicon.subscriptionapi.dto.request.SubscriptionRequest;
import se.lexicon.subscriptionapi.dto.response.SubscriptionResponse;
import se.lexicon.subscriptionapi.service.SubscriptionService;

import java.util.List;

@Tag(name = "Subscriptions", description = "Subscription management endpoints.")
@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    // 1. CREATE: Subscribe a customer to a plan
    @PostMapping("/customer/{customerId}")
    @Operation(summary = "Subscribe customer to a plan", description = "Requires JWT.\n\nRoles: USER, ADMIN")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<SubscriptionResponse> subscribe(
            @PathVariable Long customerId,
            @Valid @RequestBody SubscriptionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriptionService.subscribe(customerId, request));
    }

    // 2. UPDATE: Change plan on an existing subscription
    @PutMapping("/{id}/plan")
    @Operation(summary = "Change subscription plan", description = "Requires JWT.\n\nRoles: USER, ADMIN")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<SubscriptionResponse> changePlan(
            @PathVariable Long id,
            @Valid @RequestBody ChangePlanRequest request
    ) {
        return ResponseEntity.ok(subscriptionService.changePlan(id, request));
    }

    // 3. DELETE: Cancel an active subscription
    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel subscription", description = "Requires JWT.\n\nRoles: USER, ADMIN")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<SubscriptionResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionService.cancel(id));
    }

    // 4. READ: Get subscription by ID
    @GetMapping("/{id}")
    @Operation(summary = "Get subscription by ID", description = "Requires JWT.\n\nRoles: USER, ADMIN")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<SubscriptionResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionService.findById(id));
    }

    // 5. READ: Get all subscriptions for a specific customer
    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get subscriptions by customer ID", description = "Requires JWT.\n\nRoles: USER, ADMIN")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<SubscriptionResponse>> findByCustomerId(@PathVariable Long customerId) {
        return ResponseEntity.ok(subscriptionService.findByCustomerId(customerId));
    }

    // 6. READ: Get all subscriptions (Admin only)
    @GetMapping
    @Operation(summary = "Get all subscriptions", description = "Requires JWT.\n\nRoles: ADMIN")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SubscriptionResponse>> findAll() {
        return ResponseEntity.ok(subscriptionService.findAll());
    }
}
