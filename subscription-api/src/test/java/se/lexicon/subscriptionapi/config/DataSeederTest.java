package se.lexicon.subscriptionapi.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import se.lexicon.subscriptionapi.domain.constant.Role;
import se.lexicon.subscriptionapi.domain.constant.ServiceType;
import se.lexicon.subscriptionapi.domain.entity.Customer;
import se.lexicon.subscriptionapi.domain.entity.Operator;
import se.lexicon.subscriptionapi.domain.entity.Plan;
import se.lexicon.subscriptionapi.repository.CustomerRepository;
import se.lexicon.subscriptionapi.repository.OperatorRepository;
import se.lexicon.subscriptionapi.repository.PlanRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DataSeederTest {

    @Autowired
    private DataSeeder dataSeeder;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OperatorRepository operatorRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Should seed admin and regular users with correct roles")
    void shouldSeedUsers() {
        Optional<Customer> adminOpt = customerRepository.findByEmail("admin@example.com");
        assertThat(adminOpt).isPresent();
        Customer admin = adminOpt.get();
        assertThat(admin.getRoles()).contains(Role.ROLE_ADMIN, Role.ROLE_USER);
        assertThat(passwordEncoder.matches("password", admin.getPassword())).isTrue();

        Optional<Customer> userOpt = customerRepository.findByEmail("user@example.com");
        assertThat(userOpt).isPresent();
        Customer user = userOpt.get();
        assertThat(user.getRoles()).contains(Role.ROLE_USER);
        assertThat(passwordEncoder.matches("password", user.getPassword())).isTrue();
    }

    @Test
    @DisplayName("Should seed at least 2 operators")
    void shouldSeedOperators() {
        List<Operator> operators = operatorRepository.findAll();
        assertThat(operators).hasSizeGreaterThanOrEqualTo(2);
        assertThat(operatorRepository.findByName("Telia")).isPresent();
        assertThat(operatorRepository.findByName("Tele2")).isPresent();
    }

    @Test
    @DisplayName("Should seed multiple plans per operator with both active and inactive states")
    void shouldSeedPlansWithActiveAndInactiveStates() {
        Operator telia = operatorRepository.findByName("Telia").orElseThrow();
        Operator tele2 = operatorRepository.findByName("Tele2").orElseThrow();

        List<Plan> teliaPlans = planRepository.findByOperatorId(telia.getId());
        assertThat(teliaPlans).hasSizeGreaterThanOrEqualTo(3);
        assertThat(teliaPlans).anyMatch(Plan::isActive);
        assertThat(teliaPlans).anyMatch(p -> !p.isActive());
        assertThat(teliaPlans).anyMatch(p -> p.getServiceType() == ServiceType.INTERNET);
        assertThat(teliaPlans).anyMatch(p -> p.getServiceType() == ServiceType.MOBILE);

        List<Plan> tele2Plans = planRepository.findByOperatorId(tele2.getId());
        assertThat(tele2Plans).hasSizeGreaterThanOrEqualTo(3);
        assertThat(tele2Plans).anyMatch(Plan::isActive);
        assertThat(tele2Plans).anyMatch(p -> !p.isActive());
        assertThat(tele2Plans).anyMatch(p -> p.getServiceType() == ServiceType.INTERNET);
        assertThat(tele2Plans).anyMatch(p -> p.getServiceType() == ServiceType.MOBILE);
    }

    @Test
    @DisplayName("Should be idempotent and not create duplicate records on repeated execution")
    void shouldBeIdempotentOnRepeatedRuns() throws Exception {
        long customerCountBefore = customerRepository.count();
        long operatorCountBefore = operatorRepository.count();
        long planCountBefore = planRepository.count();

        dataSeeder.run();

        assertThat(customerRepository.count()).isEqualTo(customerCountBefore);
        assertThat(operatorRepository.count()).isEqualTo(operatorCountBefore);
        assertThat(planRepository.count()).isEqualTo(planCountBefore);
    }
}
