package se.lexicon.subscriptionapi.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.lexicon.subscriptionapi.domain.constant.ServiceType;
import se.lexicon.subscriptionapi.domain.constant.SubscriptionStatus;
import se.lexicon.subscriptionapi.domain.entity.Customer;
import se.lexicon.subscriptionapi.domain.entity.Operator;
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
import se.lexicon.subscriptionapi.service.impl.SubscriptionServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceImplTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private SubscriptionMapper subscriptionMapper;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    private Customer customer;
    private Operator operatorTelia;
    private Operator operatorTele2;
    private Plan activeMobilePlan;
    private Plan newActiveMobilePlanSameOperator;
    private Plan activeInternetPlanSameOperator;
    private Plan activeMobilePlanDifferentOperator;
    private Plan inactiveMobilePlan;
    private Subscription activeSubscription;
    private SubscriptionResponse subscriptionResponse;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setId(10L);
        customer.setEmail("customer@example.com");

        operatorTelia = new Operator(1L, "Telia", LocalDateTime.now());
        operatorTele2 = new Operator(2L, "Tele2", LocalDateTime.now());

        activeMobilePlan = new Plan(
                100L,
                operatorTelia,
                "Mobile 10GB",
                BigDecimal.valueOf(299.00),
                ServiceType.MOBILE,
                10,
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        newActiveMobilePlanSameOperator = new Plan(
                101L,
                operatorTelia,
                "Mobile 50GB",
                BigDecimal.valueOf(449.00),
                ServiceType.MOBILE,
                50,
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        activeInternetPlanSameOperator = new Plan(
                102L,
                operatorTelia,
                "Fiber 500",
                BigDecimal.valueOf(499.00),
                ServiceType.INTERNET,
                null,
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        activeMobilePlanDifferentOperator = new Plan(
                103L,
                operatorTele2,
                "Tele2 Mobile 20GB",
                BigDecimal.valueOf(349.00),
                ServiceType.MOBILE,
                20,
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        inactiveMobilePlan = new Plan(
                104L,
                operatorTelia,
                "Old Mobile Plan",
                BigDecimal.valueOf(199.00),
                ServiceType.MOBILE,
                5,
                false,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        activeSubscription = new Subscription();
        activeSubscription.setId(1L);
        activeSubscription.setCustomer(customer);
        activeSubscription.setPlan(activeMobilePlan);
        activeSubscription.setStatus(SubscriptionStatus.ACTIVE);
        activeSubscription.setStartDate(LocalDateTime.now());

        subscriptionResponse = new SubscriptionResponse(
                1L,
                10L,
                100L,
                "Mobile 10GB",
                BigDecimal.valueOf(299.00),
                ServiceType.MOBILE,
                "Telia",
                SubscriptionStatus.ACTIVE,
                LocalDateTime.now(),
                null,
                LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("Subscribe Tests")
    class SubscribeTests {

        @Test
        @DisplayName("Should subscribe successfully when plan is active and no active subscription of service type exists")
        void subscribe_Success() {
            SubscriptionRequest request = new SubscriptionRequest(100L);

            when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
            when(planRepository.findById(100L)).thenReturn(Optional.of(activeMobilePlan));
            when(subscriptionRepository.existsByCustomerIdAndPlan_ServiceTypeAndStatus(
                    10L, ServiceType.MOBILE, SubscriptionStatus.ACTIVE)).thenReturn(false);
            when(subscriptionRepository.save(any(Subscription.class))).thenReturn(activeSubscription);
            when(subscriptionMapper.toResponse(activeSubscription)).thenReturn(subscriptionResponse);

            SubscriptionResponse result = subscriptionService.subscribe(10L, request);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.planName()).isEqualTo("Mobile 10GB");
            verify(subscriptionRepository).save(any(Subscription.class));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when customerId or request is null")
        void subscribe_NullInputs_ThrowsException() {
            assertThatThrownBy(() -> subscriptionService.subscribe(null, new SubscriptionRequest(100L)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("customerId is null");

            assertThatThrownBy(() -> subscriptionService.subscribe(10L, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("request is null");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when customer not found")
        void subscribe_CustomerNotFound_ThrowsException() {
            when(customerRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> subscriptionService.subscribe(999L, new SubscriptionRequest(100L)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Customer not found");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when plan not found")
        void subscribe_PlanNotFound_ThrowsException() {
            when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
            when(planRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> subscriptionService.subscribe(10L, new SubscriptionRequest(999L)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Plan not found");
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when plan is inactive")
        void subscribe_InactivePlan_ThrowsException() {
            when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
            when(planRepository.findById(104L)).thenReturn(Optional.of(inactiveMobilePlan));

            assertThatThrownBy(() -> subscriptionService.subscribe(10L, new SubscriptionRequest(104L)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Cannot subscribe to an inactive plan");
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when customer already has active subscription of same service type")
        void subscribe_AlreadyHasActiveSubscription_ThrowsException() {
            when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
            when(planRepository.findById(100L)).thenReturn(Optional.of(activeMobilePlan));
            when(subscriptionRepository.existsByCustomerIdAndPlan_ServiceTypeAndStatus(
                    10L, ServiceType.MOBILE, SubscriptionStatus.ACTIVE)).thenReturn(true);

            assertThatThrownBy(() -> subscriptionService.subscribe(10L, new SubscriptionRequest(100L)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("already has an active subscription");
        }
    }

    @Nested
    @DisplayName("Change Plan Tests")
    class ChangePlanTests {

        @Test
        @DisplayName("Should change plan successfully when same operator and same service type")
        void changePlan_Success() {
            ChangePlanRequest request = new ChangePlanRequest(101L);

            when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(activeSubscription));
            when(planRepository.findById(101L)).thenReturn(Optional.of(newActiveMobilePlanSameOperator));
            when(subscriptionRepository.save(any(Subscription.class))).thenReturn(activeSubscription);
            when(subscriptionMapper.toResponse(any(Subscription.class))).thenReturn(subscriptionResponse);

            SubscriptionResponse result = subscriptionService.changePlan(1L, request);

            assertThat(result).isNotNull();
            assertThat(activeSubscription.getPlan()).isEqualTo(newActiveMobilePlanSameOperator);
            verify(subscriptionRepository).save(activeSubscription);
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when subscription is cancelled")
        void changePlan_CancelledSubscription_ThrowsException() {
            activeSubscription.setStatus(SubscriptionStatus.CANCELLED);
            when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(activeSubscription));
            when(planRepository.findById(101L)).thenReturn(Optional.of(newActiveMobilePlanSameOperator));

            assertThatThrownBy(() -> subscriptionService.changePlan(1L, new ChangePlanRequest(101L)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Cannot change plan on a cancelled or inactive subscription");
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when target plan is inactive")
        void changePlan_InactiveTargetPlan_ThrowsException() {
            when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(activeSubscription));
            when(planRepository.findById(104L)).thenReturn(Optional.of(inactiveMobilePlan));

            assertThatThrownBy(() -> subscriptionService.changePlan(1L, new ChangePlanRequest(104L)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Cannot change to an inactive plan");
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when operator is different")
        void changePlan_DifferentOperator_ThrowsException() {
            when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(activeSubscription));
            when(planRepository.findById(103L)).thenReturn(Optional.of(activeMobilePlanDifferentOperator));

            assertThatThrownBy(() -> subscriptionService.changePlan(1L, new ChangePlanRequest(103L)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Plan change is allowed only within the same operator");
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when service type is different")
        void changePlan_DifferentServiceType_ThrowsException() {
            when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(activeSubscription));
            when(planRepository.findById(102L)).thenReturn(Optional.of(activeInternetPlanSameOperator));

            assertThatThrownBy(() -> subscriptionService.changePlan(1L, new ChangePlanRequest(102L)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Plan change is allowed only within the same service type");
        }
    }

    @Nested
    @DisplayName("Cancel Tests")
    class CancelTests {

        @Test
        @DisplayName("Should cancel active subscription successfully")
        void cancel_Success() {
            when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(activeSubscription));
            when(subscriptionRepository.save(any(Subscription.class))).thenReturn(activeSubscription);
            when(subscriptionMapper.toResponse(any(Subscription.class))).thenReturn(subscriptionResponse);

            SubscriptionResponse result = subscriptionService.cancel(1L);

            assertThat(result).isNotNull();
            assertThat(activeSubscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
            assertThat(activeSubscription.getCancelledDate()).isNotNull();
            verify(subscriptionRepository).save(activeSubscription);
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when subscription is already cancelled")
        void cancel_AlreadyCancelled_ThrowsException() {
            activeSubscription.setStatus(SubscriptionStatus.CANCELLED);
            when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(activeSubscription));

            assertThatThrownBy(() -> subscriptionService.cancel(1L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("already cancelled");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when subscription not found")
        void cancel_NotFound_ThrowsException() {
            when(subscriptionRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> subscriptionService.cancel(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Subscription not found");
        }
    }

    @Nested
    @DisplayName("Find Tests")
    class FindTests {

        @Test
        @DisplayName("Should find subscription by ID")
        void findById_Success() {
            when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(activeSubscription));
            when(subscriptionMapper.toResponse(activeSubscription)).thenReturn(subscriptionResponse);

            SubscriptionResponse result = subscriptionService.findById(1L);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should find all subscriptions by customer ID")
        void findByCustomerId_Success() {
            when(subscriptionRepository.findByCustomerId(10L)).thenReturn(List.of(activeSubscription));
            when(subscriptionMapper.toResponse(activeSubscription)).thenReturn(subscriptionResponse);

            List<SubscriptionResponse> results = subscriptionService.findByCustomerId(10L);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should find all subscriptions")
        void findAll_Success() {
            when(subscriptionRepository.findAll()).thenReturn(List.of(activeSubscription));
            when(subscriptionMapper.toResponse(activeSubscription)).thenReturn(subscriptionResponse);

            List<SubscriptionResponse> results = subscriptionService.findAll();

            assertThat(results).hasSize(1);
        }
    }
}
