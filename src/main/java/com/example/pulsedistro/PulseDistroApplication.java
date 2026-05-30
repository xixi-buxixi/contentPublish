package com.example.pulsedistro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PulseDistroApplication {

    public static void main(String[] args) {
        SpringApplication.run(PulseDistroApplication.class, args);
    }
}
