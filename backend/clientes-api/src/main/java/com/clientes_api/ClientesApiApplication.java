package com.clientes_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ClientesApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClientesApiApplication.class, args);
	}

}
