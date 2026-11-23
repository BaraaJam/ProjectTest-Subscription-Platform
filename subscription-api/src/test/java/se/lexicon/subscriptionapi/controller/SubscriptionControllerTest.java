package se.lexicon.subscriptionapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import se.lexicon.subscriptionapi.domain.constant.ServiceType;
import se.lexicon.subscriptionapi.domain.constant.SubscriptionStatus;
import se.lexicon.subscriptionapi.dto.request.ChangePlanRequest;
import se.lexicon.subscriptionapi.dto.request.SubscriptionRequest;
import se.lexicon.subscriptionapi.dto.response.SubscriptionResponse;
import se.lexicon.subscriptionapi.exception.BusinessRuleException;
import se.lexicon.subscriptionapi.exception.GlobalExceptionHandler;
import se.lexicon.subscriptionapi.exception.ResourceNotFoundException;
import se.lexicon.subscriptionapi.service.SubscriptionService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionController Web/HTTP Layer Tests")
class SubscriptionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private SubscriptionController subscriptionController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private SubscriptionResponse standardResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(subscriptionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        standardResponse = new SubscriptionResponse(
                1L,
                10L,
                100L,
                "Fiber 500",
                new BigDecimal("499.00"),
                ServiceType.INTERNET,
                "Telia",
                SubscriptionStatus.ACTIVE,
                LocalDateTime.now(),
                null,
                LocalDateTime.now()
        );
    }

    // =========================================================================
    // 1. POST /api/v1/subscriptions/customer/{customerId} - Subscribe
    // =========================================================================
    @Nested
    @DisplayName("POST /api/v1/subscriptions/customer/{customerId} - Subscribe")
    class SubscribeEndpoints {

        @Test
        @DisplayName("Should return 201 Created when subscription is created successfully")
        void subscribe_Success_Returns201() throws Exception {
            SubscriptionRequest request = new SubscriptionRequest(100L);
            when(subscriptionService.subscribe(eq(10L), any(SubscriptionRequest.class)))
                    .thenReturn(standardResponse);

            mockMvc.perform(post("/api/v1/subscriptions/customer/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.customerId").value(10L))
                    .andExpect(jsonPath("$.planId").value(100L))
                    .andExpect(jsonPath("$.planName").value("Fiber 500"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));

            verify(subscriptionService, times(1)).subscribe(eq(10L), any(SubscriptionRequest.class));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when planId is null in request body")
        void subscribe_InvalidRequest_Returns400() throws Exception {
            String invalidJson = "{}";

            mockMvc.perform(post("/api/v1/subscriptions/customer/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("Validation failed")));

            verify(subscriptionService, never()).subscribe(any(), any());
        }

        @Test
        @DisplayName("Should return 404 Not Found when customer or plan does not exist")
        void subscribe_NotFound_Returns404() throws Exception {
            SubscriptionRequest request = new SubscriptionRequest(999L);
            when(subscriptionService.subscribe(eq(10L), any(SubscriptionRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Plan not found with id: 999"));

            mockMvc.perform(post("/api/v1/subscriptions/customer/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Plan not found with id: 999"));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when business rule is violated (duplicate service type)")
        void subscribe_DuplicateServiceType_Returns400() throws Exception {
            SubscriptionRequest request = new SubscriptionRequest(100L);
            when(subscriptionService.subscribe(eq(10L), any(SubscriptionRequest.class)))
                    .thenThrow(new BusinessRuleException("Customer already has an active subscription for service type: INTERNET"));

            mockMvc.perform(post("/api/v1/subscriptions/customer/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("already has an active subscription")));
        }
    }

    // =========================================================================
    // 2. PUT /api/v1/subscriptions/{id}/plan - Change Plan
    // =========================================================================
    @Nested
    @DisplayName("PUT /api/v1/subscriptions/{id}/plan - Change Plan")
    class ChangePlanEndpoints {

        @Test
        @DisplayName("Should return 200 OK when plan change is successful")
        void changePlan_Success_Returns200() throws Exception {
            ChangePlanRequest request = new ChangePlanRequest(200L);
            SubscriptionResponse updatedResponse = new SubscriptionResponse(
                    1L, 10L, 200L, "Fiber 1000", new BigDecimal("699.00"),
                    ServiceType.INTERNET, "Telia", SubscriptionStatus.ACTIVE,
                    LocalDateTime.now(), null, LocalDateTime.now()
            );

            when(subscriptionService.changePlan(eq(1L), any(ChangePlanRequest.class)))
                    .thenReturn(updatedResponse);

            mockMvc.perform(put("/api/v1/subscriptions/1/plan")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.planId").value(200L))
                    .andExpect(jsonPath("$.planName").value("Fiber 1000"));

            verify(subscriptionService, times(1)).changePlan(eq(1L), any(ChangePlanRequest.class));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when newPlanId is missing in body")
        void changePlan_InvalidRequest_Returns400() throws Exception {
            String invalidJson = "{}";

            mockMvc.perform(put("/api/v1/subscriptions/1/plan")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("Validation failed")));

            verify(subscriptionService, never()).changePlan(any(), any());
        }

        @Test
        @DisplayName("Should return 404 Not Found when subscription does not exist")
        void changePlan_NotFound_Returns404() throws Exception {
            ChangePlanRequest request = new ChangePlanRequest(200L);
            when(subscriptionService.changePlan(eq(999L), any(ChangePlanRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Subscription not found with id: 999"));

            mockMvc.perform(put("/api/v1/subscriptions/999/plan")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Subscription not found with id: 999"));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when operator does not match")
        void changePlan_DifferentOperator_Returns400() throws Exception {
            ChangePlanRequest request = new ChangePlanRequest(200L);
            when(subscriptionService.changePlan(eq(1L), any(ChangePlanRequest.class)))
                    .thenThrow(new BusinessRuleException("Plan change is allowed only within the same operator"));

            mockMvc.perform(put("/api/v1/subscriptions/1/plan")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("only within the same operator")));
        }
    }

    // =========================================================================
    // 3. DELETE /api/v1/subscriptions/{id} - Cancel Subscription
    // =========================================================================
    @Nested
    @DisplayName("DELETE /api/v1/subscriptions/{id} - Cancel Subscription")
    class CancelEndpoints {

        @Test
        @DisplayName("Should return 200 OK and cancelled response when cancellation succeeds")
        void cancel_Success_Returns200() throws Exception {
            SubscriptionResponse cancelledResponse = new SubscriptionResponse(
                    1L, 10L, 100L, "Fiber 500", new BigDecimal("499.00"),
                    ServiceType.INTERNET, "Telia", SubscriptionStatus.CANCELLED,
                    LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now()
            );

            when(subscriptionService.cancel(1L)).thenReturn(cancelledResponse);

            mockMvc.perform(delete("/api/v1/subscriptions/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.status").value("CANCELLED"));

            verify(subscriptionService, times(1)).cancel(1L);
        }

        @Test
        @DisplayName("Should return 404 Not Found when cancelling nonexistent subscription")
        void cancel_NotFound_Returns404() throws Exception {
            when(subscriptionService.cancel(999L))
                    .thenThrow(new ResourceNotFoundException("Subscription not found with id: 999"));

            mockMvc.perform(delete("/api/v1/subscriptions/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Subscription not found with id: 999"));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when subscription is already cancelled")
        void cancel_AlreadyCancelled_Returns400() throws Exception {
            when(subscriptionService.cancel(1L))
                    .thenThrow(new BusinessRuleException("Cannot cancel subscription. Current status is: CANCELLED"));

            mockMvc.perform(delete("/api/v1/subscriptions/1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("Cannot cancel subscription")));
        }
    }

    // =========================================================================
    // 4. GET /api/v1/subscriptions/{id} - Find By ID
    // =========================================================================
    @Nested
    @DisplayName("GET /api/v1/subscriptions/{id} - Find By ID")
    class FindByIdEndpoints {

        @Test
        @DisplayName("Should return 200 OK and subscription when ID exists")
        void findById_Success_Returns200() throws Exception {
            when(subscriptionService.findById(1L)).thenReturn(standardResponse);

            mockMvc.perform(get("/api/v1/subscriptions/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.planName").value("Fiber 500"));

            verify(subscriptionService, times(1)).findById(1L);
        }

        @Test
        @DisplayName("Should return 404 Not Found when ID does not exist")
        void findById_NotFound_Returns404() throws Exception {
            when(subscriptionService.findById(999L))
                    .thenThrow(new ResourceNotFoundException("Subscription not found with id: 999"));

            mockMvc.perform(get("/api/v1/subscriptions/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Subscription not found with id: 999"));
        }
    }

    // =========================================================================
    // 5. GET /api/v1/subscriptions/customer/{customerId} - Find by Customer ID
    // =========================================================================
    @Nested
    @DisplayName("GET /api/v1/subscriptions/customer/{customerId} - Find by Customer ID")
    class FindByCustomerIdEndpoints {

        @Test
        @DisplayName("Should return 200 OK and list of customer's subscriptions")
        void findByCustomerId_Success_Returns200() throws Exception {
            when(subscriptionService.findByCustomerId(10L)).thenReturn(List.of(standardResponse));

            mockMvc.perform(get("/api/v1/subscriptions/customer/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].customerId").value(10L));

            verify(subscriptionService, times(1)).findByCustomerId(10L);
        }
    }

    // =========================================================================
    // 6. GET /api/v1/subscriptions - Find All
    // =========================================================================
    @Nested
    @DisplayName("GET /api/v1/subscriptions - Find All")
    class FindAllEndpoints {

        @Test
        @DisplayName("Should return 200 OK and list of all subscriptions")
        void findAll_Success_Returns200() throws Exception {
            when(subscriptionService.findAll()).thenReturn(List.of(standardResponse));

            mockMvc.perform(get("/api/v1/subscriptions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(1L));

            verify(subscriptionService, times(1)).findAll();
        }
    }
}
