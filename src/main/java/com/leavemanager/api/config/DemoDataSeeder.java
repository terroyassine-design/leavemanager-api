package com.leavemanager.api.config;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.leavemanager.api.leave.LeaveRequest;
import com.leavemanager.api.leave.LeaveRequestRepository;
import com.leavemanager.api.leave.LeaveType;
import com.leavemanager.api.user.Role;
import com.leavemanager.api.user.User;
import com.leavemanager.api.user.UserRepository;

/**
 * Fills an empty database with demo accounts so a visitor can try the app right away.
 * Disabled with DEMO_DATA_ENABLED=false.
 */
@Component
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {

    public static final String DEMO_PASSWORD = "Demo1234!";

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserRepository userRepository;
    private final LeaveRequestRepository leaveRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(UserRepository userRepository, LeaveRequestRepository leaveRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.leaveRepository = leaveRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        User manager = createUser("Claire Martin", "manager@demo.com", Role.MANAGER);
        User lucas = createUser("Lucas Bernard", "employee@demo.com", Role.EMPLOYEE);
        User emma = createUser("Emma Petit", "emma@demo.com", Role.EMPLOYEE);

        LocalDate nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));

        LeaveRequest pastVacation = new LeaveRequest(lucas, LeaveType.PAID,
                nextMonday.minusWeeks(6), nextMonday.minusWeeks(6).plusDays(4), "Vacances d'été");
        pastVacation.approve(manager, "Bonnes vacances !");

        leaveRepository.save(pastVacation);
        leaveRepository.save(new LeaveRequest(lucas, LeaveType.PAID,
                nextMonday.plusWeeks(3), nextMonday.plusWeeks(3).plusDays(4), "Voyage en famille"));
        leaveRepository.save(new LeaveRequest(emma, LeaveType.UNPAID,
                nextMonday.plusWeeks(1), nextMonday.plusWeeks(1).plusDays(1), "Déménagement"));

        log.info("Demo data created: manager@demo.com / employee@demo.com (password: {})", DEMO_PASSWORD);
    }

    private User createUser(String fullName, String email, Role role) {
        User user = new User(fullName, email, passwordEncoder.encode(DEMO_PASSWORD), role);
        user.markAsDemo();
        return userRepository.save(user);
    }
}
