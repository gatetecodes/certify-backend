package com.irembo.certify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CertifyApplication {

    public static void main(String[] args) {
        SpringApplication.run(CertifyApplication.class, args);
    }
}
