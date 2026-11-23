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
import se.lexicon.subscriptionapi.domain.constant.ServiceType;
import se.lexicon.subscriptionapi.domain.entity.Plan;
import se.lexicon.subscriptionapi.dto.request.PlanRequest;
import se.lexicon.subscriptionapi.dto.response.PlanResponse;
import se.lexicon.subscriptionapi.service.PlanService;

import java.util.List;

@Tag(name = "Plans", description = "Plan management endpoints.")
@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class PlanController {

    private final PlanService planService;

    // 1. CREATE PLAN (Admin only)
    @PostMapping
    @Operation(summary = "Create plan", description = "Requires JWT.\n\nRoles: ADMIN")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlanResponse> create(@Valid @RequestBody PlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planService.create(request));
    }

    // 2. UPDATE PLAN (Admin only)
    @PutMapping("/{id}")
    @Operation(summary = "Update plan", description = "Requires JWT.\n\nRoles: ADMIN")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlanResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PlanRequest request
    ) {
        return ResponseEntity.ok(planService.update(id, request));
    }

    // 3. DELETE (Soft Delete) PLAN (Admin only)
    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete plan (deactivate)", description = "Requires JWT.\n\nRoles: ADMIN")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlanResponse> delete(@PathVariable Long id) {
        return ResponseEntity.ok(planService.delete(id));
    }

    // 4. GET PLAN BY ID (User or Admin)
    @GetMapping("/{id}")
    @Operation(summary = "Get plan by ID", description = "Requires JWT.\n\nRoles: USER, ADMIN")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PlanResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(planService.findById(id));
    }

    // 5. GET PLANS (Query with optional filters)
    @GetMapping
    @Operation(summary = "Get all plans or filter by active/serviceType/operator", description = "Requires JWT.\n\nRoles: USER, ADMIN")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<PlanResponse>> findAll(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) ServiceType serviceType,
            @RequestParam(required = false) Long operatorId
    ) {
        if (operatorId != null) {
            return ResponseEntity.ok(planService.findAllActiveByOperatorId(operatorId));
        }
        if (serviceType != null) {
            return ResponseEntity.ok(planService.findAllActiveByServiceType(serviceType));
        }
        if (Boolean.TRUE.equals(active)) {
            return ResponseEntity.ok(planService.findAllActive());
        }
        return ResponseEntity.ok(planService.findAll());
    }
}
