package com.pixelcare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class PixelCareApplication {

    public static void main(String[] args) {
        SpringApplication.run(PixelCareApplication.class, args);
    }
}
