package org.example.productService;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ProductServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner init(ProductRepository repository) {
        return args -> {
            repository.deleteAll();
            repository.save(new Product("Laptop", 4000));
            repository.save(new Product("Phone", 1000));
        };
    }
}