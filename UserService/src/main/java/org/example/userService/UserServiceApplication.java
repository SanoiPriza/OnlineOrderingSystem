package org.example.userService;

import org.example.common.security.config.CommonSecurityConfig;
import org.example.userService.model.Role;
import org.example.userService.model.User;
import org.example.userService.repository.RoleRepository;
import org.example.userService.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Set;

@SpringBootApplication
@EnableDiscoveryClient
@Import(CommonSecurityConfig.class)
@ComponentScan(
        basePackages = {
                "org.example.userService",
                "org.example.common.security"
        },
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {
                        org.example.common.security.service.AuthValidationService.class,
                        org.example.common.security.jwt.DelegatedJwtAuthenticationFilter.class
                }
        )
)
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner initDatabase(RoleRepository roleRepository,
                                   UserRepository userRepository,
                                   PasswordEncoder passwordEncoder) {
        return args -> {
            if (!roleRepository.existsByName("ADMIN")) {
                roleRepository.save(new Role("ADMIN", "Administrator with full access"));
            }

            if (!roleRepository.existsByName("USER")) {
                roleRepository.save(new Role("USER", "Regular user with limited access"));
            }

            if (!roleRepository.existsByName("MANAGER")) {
                roleRepository.save(new Role("MANAGER", "Manager with elevated access"));
            }

            if (!userRepository.existsByUsername("admin")) {
                User admin = new User(
                        "admin",
                        passwordEncoder.encode("admin123"),
                        "admin@example.com",
                        "System",
                        "Administrator"
                );

                Set<Role> adminRoles = new HashSet<>();
                adminRoles.add(roleRepository.findByName("ADMIN").get());
                admin.setRoles(adminRoles);

                userRepository.save(admin);
            }
        };
    }
}