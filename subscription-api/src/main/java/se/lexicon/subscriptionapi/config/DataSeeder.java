package se.lexicon.subscriptionapi.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.lexicon.subscriptionapi.domain.constant.Role;
import se.lexicon.subscriptionapi.domain.constant.ServiceType;
import se.lexicon.subscriptionapi.domain.entity.Customer;
import se.lexicon.subscriptionapi.domain.entity.Operator;
import se.lexicon.subscriptionapi.domain.entity.Plan;
import se.lexicon.subscriptionapi.repository.CustomerRepository;
import se.lexicon.subscriptionapi.repository.OperatorRepository;
import se.lexicon.subscriptionapi.repository.PlanRepository;

import java.math.BigDecimal;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final OperatorRepository operatorRepository;
    private final PlanRepository planRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        seedAdminUser();
        seedRegularUser();
        seedOperatorsAndPlans();
    }

    private void seedAdminUser() {
        String adminEmail = "admin@example.com";
        if (!customerRepository.existsByEmail(adminEmail)) {
            Customer admin = new Customer();
            admin.setEmail(adminEmail);
            admin.setFirstName("Admin");
            admin.setLastName("Adminson");
            admin.setPassword(passwordEncoder.encode("password"));
            admin.setRoles(Set.of(Role.ROLE_ADMIN, Role.ROLE_USER));
            customerRepository.save(admin);
            System.out.println("[DATA_SEED] Admin user created: " + adminEmail);
        }
    }

    private void seedRegularUser() {
        String userEmail = "user@example.com";
        if (!customerRepository.existsByEmail(userEmail)) {
            Customer user = new Customer();
            user.setEmail(userEmail);
            user.setFirstName("User");
            user.setLastName("Userson");
            user.setPassword(passwordEncoder.encode("password"));
            user.setRoles(Set.of(Role.ROLE_USER));
            customerRepository.save(user);
            System.out.println("[DATA_SEED] Regular user created: " + userEmail);
        }
    }

    private void seedOperatorsAndPlans() {
        // 1. Seed Telia
        Operator telia = operatorRepository.findByName("Telia")
                .orElseGet(() -> {
                    Operator op = new Operator();
                    op.setName("Telia");
                    return operatorRepository.save(op);
                });

        // 2. Seed Tele2
        Operator tele2 = operatorRepository.findByName("Tele2")
                .orElseGet(() -> {
                    Operator op = new Operator();
                    op.setName("Tele2");
                    return operatorRepository.save(op);
                });

        // 3. Seed Plans for Telia (Active & Inactive, Internet & Mobile)
        seedPlan(telia, "Telia Fiber 250", new BigDecimal("399.00"), ServiceType.INTERNET, null, true);
        seedPlan(telia, "Telia Fiber 500", new BigDecimal("549.00"), ServiceType.INTERNET, null, true);
        seedPlan(telia, "Telia Mobile 20GB", new BigDecimal("299.00"), ServiceType.MOBILE, 20, true);
        seedPlan(telia, "Telia Legacy ADSL", new BigDecimal("199.00"), ServiceType.INTERNET, null, false); // Inactive

        // 4. Seed Plans for Tele2 (Active & Inactive, Internet & Mobile)
        seedPlan(tele2, "Tele2 Bredband 300", new BigDecimal("349.00"), ServiceType.INTERNET, null, true);
        seedPlan(tele2, "Tele2 Mobil Unlimited", new BigDecimal("399.00"), ServiceType.MOBILE, 100, true);
        seedPlan(tele2, "Tele2 Mobil 5GB", new BigDecimal("149.00"), ServiceType.MOBILE, 5, true);
        seedPlan(tele2, "Tele2 Mobile 3G Old", new BigDecimal("99.00"), ServiceType.MOBILE, 2, false); // Inactive
    }

    private void seedPlan(Operator operator, String name, BigDecimal price, ServiceType serviceType, Integer dataLimit, boolean active) {
        if (!planRepository.existsByName(name)) {
            Plan plan = new Plan();
            plan.setOperator(operator);
            plan.setName(name);
            plan.setPrice(price);
            plan.setServiceType(serviceType);
            plan.setDataLimit(dataLimit);
            plan.setActive(active);
            planRepository.save(plan);
            System.out.println("[DATA_SEED] Seeded plan: " + name + " (Active: " + active + ")");
        }
    }
}