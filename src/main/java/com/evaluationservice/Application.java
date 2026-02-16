package com.evaluationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main application class for the Evaluation Service.
 * This service handles evaluation requests, scoring, and result management.
 */
@SpringBootApplication
@EnableAsync
@EnableFeignClients
public class Application {

  static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}