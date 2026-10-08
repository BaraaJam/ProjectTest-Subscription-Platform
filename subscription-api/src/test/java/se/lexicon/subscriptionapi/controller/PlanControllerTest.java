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
import se.lexicon.subscriptionapi.dto.request.PlanRequest;
import se.lexicon.subscriptionapi.dto.response.PlanResponse;
import se.lexicon.subscriptionapi.exception.BusinessRuleException;
import se.lexicon.subscriptionapi.exception.GlobalExceptionHandler;
import se.lexicon.subscriptionapi.exception.ResourceNotFoundException;
import se.lexicon.subscriptionapi.service.PlanService;

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
@DisplayName("PlanController Web/HTTP Layer Tests")
class PlanControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PlanService planService;

    @InjectMocks
    private PlanController planController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private PlanRequest validRequest;
    private PlanResponse standardResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(planController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        validRequest = new PlanRequest(
                "Fiber 500",
                new BigDecimal("499.00"),
                ServiceType.INTERNET,
                500,
                true,
                1L
        );

        standardResponse = new PlanResponse(
                1L,
                "Fiber 500",
                new BigDecimal("499.00"),
                ServiceType.INTERNET,
                500,
                true,
                1L,
                "Telia",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("POST /api/v1/plans - Create Plan")
    class CreateEndpoints {

        @Test
        @DisplayName("Should return 201 Created when request is valid")
        void create_ValidRequest_Returns201() throws Exception {
            when(planService.create(any(PlanRequest.class))).thenReturn(standardResponse);

            mockMvc.perform(post("/api/v1/plans")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Fiber 500"))
                    .andExpect(jsonPath("$.operatorName").value("Telia"))
                    .andExpect(jsonPath("$.active").value(true));

            verify(planService).create(any(PlanRequest.class));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when validation fails (e.g. blank name)")
        void create_InvalidRequest_Returns400() throws Exception {
            PlanRequest invalidRequest = new PlanRequest(
                    "", // Blank name violates @NotBlank
                    new BigDecimal("499.00"),
                    ServiceType.INTERNET,
                    500,
                    true,
                    1L
            );

            mockMvc.perform(post("/api/v1/plans")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Validation failed"));

            verify(planService, never()).create(any());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when service throws BusinessRuleException")
        void create_DuplicateName_Returns400() throws Exception {
            when(planService.create(any(PlanRequest.class)))
                    .thenThrow(new BusinessRuleException("Plan name already exists: Fiber 500"));

            mockMvc.perform(post("/api/v1/plans")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value(containsString("Plan name already exists")));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/plans/{id} - Get Plan By ID")
    class GetByIdEndpoints {

        @Test
        @DisplayName("Should return 200 OK with PlanResponse when found")
        void getById_Found_Returns200() throws Exception {
            when(planService.findById(1L)).thenReturn(standardResponse);

            mockMvc.perform(get("/api/v1/plans/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Fiber 500"));

            verify(planService).findById(1L);
        }

        @Test
        @DisplayName("Should return 404 Not Found when plan does not exist")
        void getById_NotFound_Returns404() throws Exception {
            when(planService.findById(99L))
                    .thenThrow(new ResourceNotFoundException("Plan not found with id: 99"));

            mockMvc.perform(get("/api/v1/plans/{id}", 99L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value(containsString("Plan not found with id: 99")));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/plans - Find All & Filters")
    class FindAllEndpoints {

        @Test
        @DisplayName("Should return 200 OK with all plans when no filters provided")
        void findAll_NoFilters_ReturnsAll() throws Exception {
            when(planService.findAll()).thenReturn(List.of(standardResponse));

            mockMvc.perform(get("/api/v1/plans"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name").value("Fiber 500"));

            verify(planService).findAll();
        }

        @Test
        @DisplayName("Should return 200 OK with active plans when ?active=true")
        void findAll_ActiveTrue_ReturnsActive() throws Exception {
            when(planService.findAllActive()).thenReturn(List.of(standardResponse));

            mockMvc.perform(get("/api/v1/plans").param("active", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(planService).findAllActive();
        }

        @Test
        @DisplayName("Should return 200 OK filtered by service type when ?serviceType=INTERNET")
        void findAll_ServiceType_ReturnsFiltered() throws Exception {
            when(planService.findAllActiveByServiceType(ServiceType.INTERNET))
                    .thenReturn(List.of(standardResponse));

            mockMvc.perform(get("/api/v1/plans").param("serviceType", "INTERNET"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(planService).findAllActiveByServiceType(ServiceType.INTERNET);
        }

        @Test
        @DisplayName("Should return 200 OK filtered by operatorId when ?operatorId=1")
        void findAll_OperatorId_ReturnsFiltered() throws Exception {
            when(planService.findAllActiveByOperatorId(1L))
                    .thenReturn(List.of(standardResponse));

            mockMvc.perform(get("/api/v1/plans").param("operatorId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(planService).findAllActiveByOperatorId(1L);
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/plans/{id} - Update Plan")
    class UpdateEndpoints {

        @Test
        @DisplayName("Should return 200 OK when update is valid")
        void update_ValidRequest_Returns200() throws Exception {
            when(planService.update(eq(1L), any(PlanRequest.class))).thenReturn(standardResponse);

            mockMvc.perform(put("/api/v1/plans/{id}", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Fiber 500"));

            verify(planService).update(eq(1L), any(PlanRequest.class));
        }

        @Test
        @DisplayName("Should return 404 Not Found when updating non-existent plan")
        void update_NotFound_Returns404() throws Exception {
            when(planService.update(eq(99L), any(PlanRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Plan not found with id: 99"));

            mockMvc.perform(put("/api/v1/plans/{id}", 99L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/plans/{id} - Soft Delete Plan")
    class DeleteEndpoints {

        @Test
        @DisplayName("Should return 200 OK with deactivated plan")
        void delete_ExistingPlan_Returns200() throws Exception {
            PlanResponse deactivated = new PlanResponse(
                    1L,
                    "Fiber 500",
                    new BigDecimal("499.00"),
                    ServiceType.INTERNET,
                    500,
                    false, // now inactive
                    1L,
                    "Telia",
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );

            when(planService.delete(1L)).thenReturn(deactivated);

            mockMvc.perform(delete("/api/v1/plans/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.active").value(false));

            verify(planService).delete(1L);
        }

        @Test
        @DisplayName("Should return 404 Not Found when deleting non-existent plan")
        void delete_NotFound_Returns404() throws Exception {
            when(planService.delete(99L))
                    .thenThrow(new ResourceNotFoundException("Plan not found with id: 99"));

            mockMvc.perform(delete("/api/v1/plans/{id}", 99L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }
}
