package com.salestorm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SaleStormApplication {
    public static void main(String[] args) {
        SpringApplication.run(SaleStormApplication.class, args);
    }
}
