package org.example.adminService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import javafx.application.Application;
import org.example.adminService.ui.AdminDashboardApp;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication(scanBasePackages = {"org.example.adminService", "org.example.common"})
@EnableDiscoveryClient
public class AdminServiceApplication {
    public static void main(String[] args) {
        org.springframework.boot.builder.SpringApplicationBuilder builder = 
                new org.springframework.boot.builder.SpringApplicationBuilder(AdminServiceApplication.class);
        builder.headless(false);
        ConfigurableApplicationContext ctx = builder.run(args);
        if (!java.awt.GraphicsEnvironment.isHeadless()) {
            Application.launch(AdminDashboardApp.class, args);
            SpringApplication.exit(ctx, () -> 0);
            System.exit(0);
        }
    }
}
