package com.sait.peelin;

import com.sait.peelin.config.EnvValidator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(Application.class);
		// Registered imperatively (not via spring.factories) so the fail-fast env banner always
		// prints before Spring tries to resolve @Value placeholders like ${JWT_SECRET}.
		app.addListeners(new EnvValidator());
		app.run(args);
	}

}
