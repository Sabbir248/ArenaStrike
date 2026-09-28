package com.arenastrike;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Entry point for the ArenaStrike backend.
 */
@SpringBootApplication
@EnableJpaAuditing
public class ArenaStrikeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArenaStrikeApplication.class, args);
    }
}
