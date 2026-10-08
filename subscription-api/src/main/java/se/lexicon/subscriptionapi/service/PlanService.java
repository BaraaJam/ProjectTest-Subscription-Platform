package se.lexicon.subscriptionapi.service;

import se.lexicon.subscriptionapi.domain.constant.ServiceType;
import se.lexicon.subscriptionapi.dto.request.PlanRequest;
import se.lexicon.subscriptionapi.dto.response.PlanResponse;

import java.util.List;

public interface PlanService {
    PlanResponse create(PlanRequest planRequest);
    PlanResponse update(Long id, PlanRequest planRequest);
    PlanResponse delete(Long id);
    List<PlanResponse> findAll();
    PlanResponse findById(Long id);
    List<PlanResponse> findAllActive();
    List<PlanResponse> findAllActiveByServiceType(ServiceType serviceType);
    List<PlanResponse> findAllActiveByOperatorId(Long operatorId);
}
