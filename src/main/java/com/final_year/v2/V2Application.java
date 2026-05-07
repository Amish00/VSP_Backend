package com.final_year.v2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableScheduling
@EnableTransactionManagement
@EntityScan(basePackages = "com.final_year.v2.model")   // scan entities
@EnableJpaRepositories(basePackages = "com.final_year.v2.repository")
public class V2Application {

    public static void main(String[] args) {
        SpringApplication.run(V2Application.class, args);
    }

}
