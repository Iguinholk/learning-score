package br.com.learningscore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Ponto de entrada do Learning Score (monólito modular). */
@SpringBootApplication
@EnableScheduling
public class LearningScoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(LearningScoreApplication.class, args);
    }
}
