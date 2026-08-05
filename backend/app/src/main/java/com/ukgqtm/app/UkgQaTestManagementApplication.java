package com.ukgqtm.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication(scanBasePackages = "com.ukgqtm")
@AutoConfigurationPackage(basePackages = "com.ukgqtm")
@EntityScan(basePackages = "com.ukgqtm")
public class UkgQaTestManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(UkgQaTestManagementApplication.class, args);
    }
}
