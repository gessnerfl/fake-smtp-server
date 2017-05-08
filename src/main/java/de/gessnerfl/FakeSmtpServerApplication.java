package de.gessnerfl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FakeSmtpServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(FakeSmtpServerApplication.class, args);
	}
}
