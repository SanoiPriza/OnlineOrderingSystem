  package org.example.common.security.config;

  import org.springframework.context.annotation.Bean;
  import org.springframework.context.annotation.Configuration;
  import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
  @Configuration
  public class ActuatorSecurityConfig {

      @Bean
      public WebSecurityCustomizer actuatorWebSecurityCustomizer() {
          return web -> web.ignoring().requestMatchers("/actuator/**");
      }
  }