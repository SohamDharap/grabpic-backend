package com.grabpic.backend.config;

import com.grabpic.backend.entity.UserDetails;
import com.grabpic.backend.enums.GenderType;
import com.grabpic.backend.enums.UserRole;
import com.grabpic.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        String adminEmail = "sohamdharap9@gmail.com";

        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            UserDetails admin = UserDetails.builder()
                    .firstname("Soham")
                    .lastname("Dharap")
                    .email(adminEmail)
                    .phoneNumber("9999999999")
                    .role(UserRole.ADMIN)
                    .gender(GenderType.MALE)
                    .age(25)
                    .isActive(true)
                    .build();

            userRepository.save(admin);
            log.info("=================================================");
            log.info("  CREATED SUPERADMIN ACCOUNT: {}", adminEmail);
            log.info("=================================================");
        } else {
            log.info("Superadmin account [{}] already exists.", adminEmail);
        }
    }
}
