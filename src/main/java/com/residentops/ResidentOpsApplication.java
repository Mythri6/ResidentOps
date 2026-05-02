package com.residentops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ResidentOpsApplication {
    public static void main(String[] args) {
        SpringApplication.run(ResidentOpsApplication.class, args);
    }
}
