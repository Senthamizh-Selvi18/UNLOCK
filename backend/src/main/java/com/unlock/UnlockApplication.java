package com.unlock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * UNLOCK backend - Week 1 goal:
 * 1. A student can log in with GitHub.
 * 2. Their basic profile gets saved into MongoDB.
 * 3. The frontend can ask "who am I?" and get an answer.
 *
 * That's it for this week. Nothing else.
 */
@SpringBootApplication
@EnableScheduling
public class UnlockApplication {
    public static void main(String[] args) {
        SpringApplication.run(UnlockApplication.class, args);
    }
}
