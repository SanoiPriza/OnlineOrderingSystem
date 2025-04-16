package org.example.userService.config;

import org.example.common.security.config.CommonSecurityConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(CommonSecurityConfig.class)
public class UserServiceSecurityConfig {
}
