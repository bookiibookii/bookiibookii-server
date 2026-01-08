package com.example.bookiibookii;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class BookiibookiiApplication {

	public static void main(String[] args) {
		SpringApplication.run(BookiibookiiApplication.class, args);
	}

}
