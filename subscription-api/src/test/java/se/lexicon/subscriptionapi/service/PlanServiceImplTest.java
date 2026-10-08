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
import se.lexicon.subscriptionapi.domain.entity.Operator;
import se.lexicon.subscriptionapi.domain.entity.Plan;
import se.lexicon.subscriptionapi.dto.request.PlanRequest;
import se.lexicon.subscriptionapi.dto.response.PlanResponse;
import se.lexicon.subscriptionapi.exception.BusinessRuleException;
import se.lexicon.subscriptionapi.exception.ResourceNotFoundException;
import se.lexicon.subscriptionapi.mapper.PlanMapper;
import se.lexicon.subscriptionapi.repository.OperatorRepository;
import se.lexicon.subscriptionapi.repository.PlanRepository;
import se.lexicon.subscriptionapi.service.impl.PlanServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PlanServiceImpl using Mockito.
 * Demonstrates the Arrange-Act-Assert (AAA) pattern and isolation from the database/Spring container.
 */
@ExtendWith(MockitoExtension.class)
class PlanServiceImplTest {

    @Mock
    private PlanRepository planRepository;

    @Mock
    private OperatorRepository operatorRepository;

    @Mock
    private PlanMapper planMapper;

    @InjectMocks
    private PlanServiceImpl planService;

    private Operator operator;
    private Plan plan;
    private PlanRequest planRequest;
    private PlanResponse planResponse;

    @BeforeEach
    void setUp() {
        operator = new Operator(1L, "Telia", LocalDateTime.now());

        plan = new Plan(
                1L,
                operator,
                "Unlimited 5G",
                BigDecimal.valueOf(499.00),
                ServiceType.MOBILE,
                null,
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        planRequest = new PlanRequest(
                "Unlimited 5G",
                BigDecimal.valueOf(499.00),
                ServiceType.MOBILE,
                null,
                true,
                1L
        );

        planResponse = new PlanResponse(
                1L,
                "Unlimited 5G",
                BigDecimal.valueOf(499.00),
                ServiceType.MOBILE,
                null,
                true,
                1L,
                "Telia",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    // =========================================================================
    // 1. CREATE TESTS
    // =========================================================================
    @Nested
    @DisplayName("Create Plan Tests")
    class CreateTests {

        @Test
        @DisplayName("Should successfully create a plan when request is valid")
        void create_Success() {
            // Arrange
            when(planRepository.existsByName(planRequest.name())).thenReturn(false);
            when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
            when(planMapper.toEntity(planRequest)).thenReturn(plan);
            when(planRepository.save(plan)).thenReturn(plan);
            when(planMapper.toResponse(plan)).thenReturn(planResponse);

            // Act
            PlanResponse result = planService.create(planRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.name()).isEqualTo("Unlimited 5G");
            assertThat(result.operatorName()).isEqualTo("Telia");

            verify(planRepository).existsByName(planRequest.name());
            verify(operatorRepository).findById(1L);
            verify(planRepository).save(plan);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when request is null")
        void create_NullRequest_ThrowsException() {
            // Act & Assert
            assertThatThrownBy(() -> planService.create(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Request body is required");

            verifyNoInteractions(planRepository, operatorRepository, planMapper);
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when plan name already exists")
        void create_DuplicateName_ThrowsException() {
            // Arrange
            when(planRepository.existsByName(planRequest.name())).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> planService.create(planRequest))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Plan name already exists");

            verify(planRepository).existsByName(planRequest.name());
            verifyNoInteractions(operatorRepository);
            verify(planRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when operator does not exist")
        void create_OperatorNotFound_ThrowsException() {
            // Arrange
            when(planRepository.existsByName(planRequest.name())).thenReturn(false);
            when(operatorRepository.findById(1L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> planService.create(planRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Operator not found with id: 1");

            verify(planRepository, never()).save(any());
        }
    }

    // =========================================================================
    // 2. FIND BY ID TESTS
    // =========================================================================
    @Nested
    @DisplayName("Find By ID Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should return plan response when ID exists")
        void findById_Success() {
            // Arrange
            when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
            when(planMapper.toResponse(plan)).thenReturn(planResponse);

            // Act
            PlanResponse result = planService.findById(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            verify(planRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when ID is null")
        void findById_NullId_ThrowsException() {
            assertThatThrownBy(() -> planService.findById(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Plan ID cannot be null");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when plan is not found")
        void findById_NotFound_ThrowsException() {
            when(planRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> planService.findById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Plan not found with id: 99");
        }
    }

    // =========================================================================
    // 3. FIND ALL / FILTER TESTS
    // =========================================================================
    @Nested
    @DisplayName("Find All & Query Filter Tests")
    class FindAllTests {

        @Test
        @DisplayName("Should return all plans")
        void findAll_Success() {
            when(planRepository.findAll()).thenReturn(List.of(plan));
            when(planMapper.toResponse(plan)).thenReturn(planResponse);

            List<PlanResponse> result = planService.findAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("Unlimited 5G");
            verify(planRepository).findAll();
        }

        @Test
        @DisplayName("Should return all active plans")
        void findAllActive_Success() {
            when(planRepository.findByActiveTrue()).thenReturn(List.of(plan));
            when(planMapper.toResponse(plan)).thenReturn(planResponse);

            List<PlanResponse> result = planService.findAllActive();

            assertThat(result).hasSize(1);
            verify(planRepository).findByActiveTrue();
        }

        @Test
        @DisplayName("Should return active plans filtered by service type")
        void findAllActiveByServiceType_Success() {
            when(planRepository.findByActiveTrueAndServiceType(ServiceType.MOBILE)).thenReturn(List.of(plan));
            when(planMapper.toResponse(plan)).thenReturn(planResponse);

            List<PlanResponse> result = planService.findAllActiveByServiceType(ServiceType.MOBILE);

            assertThat(result).hasSize(1);
            verify(planRepository).findByActiveTrueAndServiceType(ServiceType.MOBILE);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when service type is null")
        void findAllActiveByServiceType_NullType_ThrowsException() {
            assertThatThrownBy(() -> planService.findAllActiveByServiceType(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should return active plans filtered by operator ID")
        void findAllActiveByOperatorId_Success() {
            when(planRepository.findByOperatorIdAndActiveTrue(1L)).thenReturn(List.of(plan));
            when(planMapper.toResponse(plan)).thenReturn(planResponse);

            List<PlanResponse> result = planService.findAllActiveByOperatorId(1L);

            assertThat(result).hasSize(1);
            verify(planRepository).findByOperatorIdAndActiveTrue(1L);
        }
    }

    // =========================================================================
    // 4. UPDATE TESTS
    // =========================================================================
    @Nested
    @DisplayName("Update Plan Tests")
    class UpdateTests {

        @Test
        @DisplayName("Should update plan fields successfully")
        void update_Success() {
            // Arrange
            PlanRequest updateReq = new PlanRequest(
                    "Unlimited 5G Plus",
                    BigDecimal.valueOf(549.00),
                    ServiceType.MOBILE,
                    null,
                    true,
                    1L
            );

            when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
            when(planRepository.existsByName("Unlimited 5G Plus")).thenReturn(false);
            when(planRepository.save(any(Plan.class))).thenReturn(plan);
            when(planMapper.toResponse(any(Plan.class))).thenReturn(planResponse);

            // Act
            PlanResponse result = planService.update(1L, updateReq);

            // Assert
            assertThat(result).isNotNull();
            verify(planRepository).findById(1L);
            verify(planRepository).save(plan);
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when updated name already exists")
        void update_DuplicateName_ThrowsException() {
            PlanRequest updateReq = new PlanRequest(
                    "Different Name",
                    BigDecimal.valueOf(549.00),
                    ServiceType.MOBILE,
                    null,
                    true,
                    1L
            );

            when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
            when(planRepository.existsByName("Different Name")).thenReturn(true);

            assertThatThrownBy(() -> planService.update(1L, updateReq))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Plan name already exists");

            verify(planRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should re-link operator when operatorId changes")
        void update_OperatorChanged_Success() {
            Operator newOperator = new Operator(2L, "Tele2", LocalDateTime.now());
            PlanRequest updateReq = new PlanRequest(
                    "Unlimited 5G", // same name, no duplicate check triggered
                    BigDecimal.valueOf(499.00),
                    ServiceType.MOBILE,
                    null,
                    true,
                    2L // changed operator
            );

            when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
            when(operatorRepository.findById(2L)).thenReturn(Optional.of(newOperator));
            when(planRepository.save(any(Plan.class))).thenReturn(plan);
            when(planMapper.toResponse(any(Plan.class))).thenReturn(planResponse);

            PlanResponse result = planService.update(1L, updateReq);

            assertThat(result).isNotNull();
            assertThat(plan.getOperator()).isEqualTo(newOperator);
            verify(operatorRepository).findById(2L);
        }
    }

    // =========================================================================
    // 5. DELETE (SOFT DELETE) TESTS
    // =========================================================================
    @Nested
    @DisplayName("Delete (Soft-Delete) Tests")
    class DeleteTests {

        @Test
        @DisplayName("Should soft-delete by setting active to false")
        void delete_Success() {
            // Arrange
            when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
            when(planRepository.save(plan)).thenReturn(plan);
            when(planMapper.toResponse(plan)).thenReturn(planResponse);

            // Act
            PlanResponse result = planService.delete(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(plan.isActive()).isFalse(); // Verifies soft delete flag
            verify(planRepository).save(plan);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when plan to delete does not exist")
        void delete_NotFound_ThrowsException() {
            when(planRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> planService.delete(404L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Plan not found with id: 404");

            verify(planRepository, never()).save(any());
        }
    }
}
