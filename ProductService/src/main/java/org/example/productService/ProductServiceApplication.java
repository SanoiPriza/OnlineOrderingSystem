package org.example.productService;

import org.example.productService.model.Product;
import org.example.productService.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;

@SpringBootApplication
@EnableDiscoveryClient
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }

    @Bean
    public CommandLineRunner init(ProductRepository repository) {
        return args -> {
            repository.deleteAll();
            repository.save(new Product("Laptop", BigDecimal.valueOf(4000)));
            repository.save(new Product("Phone", BigDecimal.valueOf(1000)));
        };
    }
}