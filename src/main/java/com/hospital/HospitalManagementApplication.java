package com.hospital;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class HospitalManagementApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(HospitalManagementApplication.class);
    }

    public static void main(String[] args) {
        com.hospital.config.LocalEnvLoader.load();
        com.hospital.config.DatabaseUrlProcessor.processSystemProperties();
        SpringApplication.run(HospitalManagementApplication.class, args);
    }

}
