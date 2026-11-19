package com.zjgsu.czh.userservice.config;

import com.zjgsu.czh.userservice.model.UserAccount;
import com.zjgsu.czh.userservice.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String ADMIN_PASSWORD_HASH =
            "$2a$10$JqAMPhz9qhAZe0y0ep60vexBElWuiuXFdgnCwejOEfmsDZREwvLn2";

    private static final String MEMBER_PASSWORD_HASH =
            "$2a$10$JqAMPhz9qhAZe0y0ep60vexBElWuiuXFdgnCwejOEfmsDZREwvLn2";

    @Bean
    public ApplicationRunner initAccounts(UserAccountRepository userAccountRepository) {
        return args -> {
            createIfAbsent(userAccountRepository, "admin", ADMIN_PASSWORD_HASH, "ADMIN");
            createIfAbsent(userAccountRepository, "member", MEMBER_PASSWORD_HASH, "MEMBER");
        };
    }

    private void createIfAbsent(
            UserAccountRepository repository,
            String username,
            String passwordHash,
            String role) {

        if (repository.existsByUsername(username)) {
            return;
        }

        repository.save(new UserAccount(username, passwordHash, role));
        log.info("Created demo account username={} role={}", username, role);
    }
}
